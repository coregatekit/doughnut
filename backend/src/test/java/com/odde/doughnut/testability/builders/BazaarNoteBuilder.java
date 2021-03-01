package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNoteBuilder {
    private final MakeMe makeMe;
    private final Note note;
    private final BazaarNote bazaarNote;

    public BazaarNoteBuilder(MakeMe makeMe, Note note) {
        this.makeMe = makeMe;
        this.note = note;
        bazaarNote = new BazaarNote();
        bazaarNote.setNote(note);
    }

    public void please(BazaarNoteRepository bazaarRepository) {
        bazaarRepository.save(bazaarNote);
    }
}
