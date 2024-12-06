package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.List;

public class GraphRAGService {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 500;
  public static final double CHARACTERS_PER_TOKEN = 3.75;

  private final PriorityLayer firstPriorityLayer;
  private int remainingBudget;

  public GraphRAGService() {
    // Create handlers
    ParentRelationshipHandler parentHandler = new ParentRelationshipHandler();
    ObjectRelationshipHandler objectHandler = new ObjectRelationshipHandler();
    ChildRelationshipHandler childrenHandler = new ChildRelationshipHandler();
    YoungerSiblingRelationshipHandler youngerSiblingHandler =
        new YoungerSiblingRelationshipHandler();

    // Set up priority layers
    PriorityLayer priorityOneLayer = new PriorityLayer(this, parentHandler, objectHandler);
    PriorityLayer priorityTwoLayer =
        new PriorityLayer(this, childrenHandler, youngerSiblingHandler);

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    this.firstPriorityLayer = priorityOneLayer;
  }

  private int estimateTokens(Note note) {
    int detailsLength =
        note.getDetails() != null
            ? Math.min(note.getDetails().length(), RELATED_NOTE_DETAILS_TRUNCATE_LENGTH)
            : 0;
    int titleLength = note.getTopicConstructor().length();
    return (int) Math.ceil((detailsLength + titleLength) / CHARACTERS_PER_TOKEN);
  }

  public BareNote addNoteToRelatedNotes(
      List<BareNote> relatedNotes, Note note, RelationshipToFocusNote relationship) {
    int tokens = estimateTokens(note);
    if (tokens <= remainingBudget) {
      BareNote bareNote = BareNote.fromNote(note, relationship);
      relatedNotes.add(bareNote);
      remainingBudget -= tokens;
      return bareNote;
    }
    return null;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    remainingBudget = tokenBudgetForRelatedNotes;
    GraphRAGResult result = new GraphRAGResult();
    FocusNote focus = FocusNote.fromNote(focusNote);
    List<BareNote> relatedNotes = new ArrayList<>();

    firstPriorityLayer.handle(focusNote, focus, relatedNotes);

    result.setFocusNote(focus);
    result.setRelatedNotes(relatedNotes);
    return result;
  }
}
