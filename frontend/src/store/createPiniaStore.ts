import { defineStore } from "pinia";
import MinimumNoteSphere from "./MinimumNoteSphere";
import Links from "./Links";


interface State {
  notebooks: Generated.NotebookViewedByUser[]
  notebooksMapByHeadNoteId: {[id: Doughnut.ID]: Generated.NotebookViewedByUser}
  noteSpheres: {[id: Doughnut.ID]: Generated.NoteSphere }
  parentChildrenIds: {[id: Doughnut.ID]: Doughnut.ID[] }
  highlightNoteId: Doughnut.ID | undefined
  noteUndoHistories: any[]
  currentUser: Generated.User | null
  featureToggle: boolean
  viewType: string
  environment: 'production' | 'testing'
}

function withState(state: State) {
  return {
    getNoteById(id: Doughnut.ID | undefined) {
      return this.getNoteSphereById(id)?.note
    },

    getNoteSphereById(id: Doughnut.ID | undefined) {
      if(id === undefined) return undefined;
      return state.noteSpheres[id]
    },

    getLinksById(id: Doughnut.ID) {
      return this.getNoteSphereById(id)?.links
    },

    getNotePosition(id: Doughnut.ID | undefined) {
      if(!id) return undefined
      const ancestors: Generated.Note[] = []
      let cursor = this.getNoteById(id)
      while(cursor && cursor.parentId) {
        cursor = this.getNoteById(cursor.parentId)
        if(!cursor) return undefined
        ancestors.unshift(cursor)
      }
      if(!cursor) return undefined
      const notebook = state.notebooksMapByHeadNoteId[cursor.id]
      return { noteId: id, ancestors, notebook } as Generated.NotePositionViewedByUser
    },

    getChildrenIdsByParentId(parentId: Doughnut.ID) {
      return state.parentChildrenIds[parentId] || []
    },

    getChildrenOfParentId(parentId: Doughnut.ID) {
      return this.getChildrenIdsByParentId(parentId)
        .map((id: Doughnut.ID)=>this.getNoteById(id))
        .filter((n: any)=>n)
    },
   
    deleteNote(id: Doughnut.ID) {
      this.getChildrenIdsByParentId(id)?.forEach((cid: Doughnut.ID)=>this.deleteNote(cid))
      delete state.noteSpheres[id]
    },

    deleteNoteFromParentChildrenList(id: Doughnut.ID) {
      const parent = this.getNoteById(id)?.parentId
      if(!parent) return
      const children = this.getChildrenIdsByParentId(parent)
      if (children) {
        const index = children.indexOf(id)
        if (index > -1) {
          children.splice(index, 1);
        }
      }
    },
  }
}

export default defineStore('main', {
    state: () => ({
        notebooks: [],
        notebooksMapByHeadNoteId: {},
        noteSpheres: {},
        parentChildrenIds: {},
        highlightNoteId: undefined,
        noteUndoHistories: [],
        currentUser: null,
        featureToggle: false,
        viewType: 'card',
        environment: 'production',
    } as State),

    getters: {
        getHighlightNote: (state: State)   => () => withState(state).getNoteById(state.highlightNoteId),
        getNoteById: (state)        => (id: Doughnut.ID) => withState(state).getNoteById(id),
        getHighlightNotePosition: (state)        => () => withState(state).getNotePosition(state.highlightNoteId),
        getNoteByIdLegacy: (state)        => (id: Doughnut.ID): MinimumNoteSphere | undefined => {
          const note = withState(state).getNoteById(id)
          if(!note) return undefined
          const {parentId} = note
          const links = withState(state).getLinksById(id)
          const childrenIds = withState(state).getChildrenIdsByParentId(id)
          return {id, parentId, links, childrenIds }
        },
        getLinksById: (state)        => (id: Doughnut.ID) => withState(state).getLinksById(id),
        peekUndo: (state)           => () => {
          if(state.noteUndoHistories.length === 0) return null
          return state.noteUndoHistories[state.noteUndoHistories.length - 1]
        },
        getChildrenIdsByParentId: (state) => (parentId: Doughnut.ID) => withState(state).getChildrenIdsByParentId(parentId),
        getChildrenOfParentId: (state)    => (parentId: Doughnut.ID) => withState(state).getChildrenOfParentId(parentId),
    },

    actions: {

        loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
          this.notebooks = notebooks
          notebooks.forEach(nb=>{ this.loadNotebook(nb) })
        },

        loadNotebook(notebook: Generated.NotebookViewedByUser) {
          this.notebooksMapByHeadNoteId[notebook.headNoteId] = notebook
        },

        addEditingToUndoHistory({noteId}: {noteId: Doughnut.ID}) {
          this.noteUndoHistories.push({type: 'editing', noteId, textContent: {...withState(this).getNoteById(noteId)?.textContent}});
        },
        popUndoHistory() {
          if (this.noteUndoHistories.length === 0) {
            return
          }
          this.noteUndoHistories.pop();
        },

        loadNoteSpheres(noteSpheres: Generated.NoteSphere[]) {
          noteSpheres.forEach((noteSphere) => {
            const {id} = noteSphere.note;
            this.noteSpheres[id] = noteSphere;
            if(noteSphere.childrenIds !== undefined) this.parentChildrenIds[id] = noteSphere.childrenIds;
          });
        },

        loadNote(id: Doughnut.ID, note: Generated.Note) {
          const noteSphere = this.noteSpheres[id];
          if (!noteSphere) {
            this.noteSpheres[id] = {id, note }
            return
          }
          noteSphere.note = note
        },


        loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
          notePosition.ancestors.forEach((note) => {
            const {id} = note;
            this.loadNote(id, note)
          });
          this.loadNotebook(notePosition.notebook)
        },

        loadNotesBulk(noteBulk: Generated.NotesBulk) {
          this.loadNoteSpheres(noteBulk.notes);
          this.loadNotePosition(noteBulk.notePosition);
        },

        loadNoteWithPosition(noteWithPosition: Generated.NoteWithPosition) {
          this.loadNoteSpheres([noteWithPosition.note]);
          this.loadNotePosition(noteWithPosition.notePosition);
        },

        deleteNote(noteId: Doughnut.ID) {
          withState(this).deleteNoteFromParentChildrenList(noteId)
          withState(this).deleteNote(noteId)
          this.noteUndoHistories.push({type: 'delete note', noteId});
        },
        setHighlightNoteId(noteId: Doughnut.ID) {
          this.highlightNoteId = noteId
        },
        setViewType(viewType: string) {
          this.viewType = viewType
        },
        setCurrentUser(user: Generated.User) {
          this.currentUser = user
        },
        setFeatureToggle(ft: boolean) {
          this.environment = "testing"
          this.featureToggle = ft
        },
      },
    });
