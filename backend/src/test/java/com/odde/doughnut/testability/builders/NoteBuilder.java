package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.testability.MakeMe;

import java.time.LocalDate;
import java.util.Date;

public class NoteBuilder {
    static TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);
    private Note note = new Note();

    public NoteBuilder(MakeMe makeMe){
        note.setTitle(titleCounter.generate());
        note.setDescription("descrption");
        note.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }
    public Note inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(User user) {
        note.setUser(user);
        return this;
    }

    public NoteBuilder under(Note parentNote) {
        parentNote.addChild(note);
        return this;
    }

    public NoteBuilder linkTo(Note referTo) {
        note.linkToNote(referTo);
        return this;
    }

    public NoteBuilder updatedAt(Date updatedDatetime) {
        note.setUpdatedDatetime(updatedDatetime);
        return this;
    }

    public Note please(NoteRepository noteRepository) {
        noteRepository.save(note);
        return note;
    }

}
