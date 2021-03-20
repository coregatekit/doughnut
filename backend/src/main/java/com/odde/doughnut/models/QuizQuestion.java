package com.odde.doughnut.models;

import com.odde.doughnut.entities.AnswerEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QuizQuestion {
    public enum QuestionType {
        CLOZE_SELECTION("cloze_selection"),
        SPELLING("spelling");

        public final String label;

        QuestionType(String label) {
            this.label = label;
        }
    }

    private final NoteEntity noteEntity;
    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;
    private final ReviewPointEntity reviewPointEntity;
    @Getter @Setter
    private QuestionType questionType = null;

    public QuizQuestion(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.noteEntity = reviewPointEntity.getNoteEntity();
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public String getClozeDescription() {
        return noteEntity.getClozeDescription();
    }

    public List<String> getOptions() {
        TreeNodeModel treeNodeModel = getTreeNodeModel();
        List<String> list = treeNodeModel.getSiblings().stream().map(NoteEntity::getTitle)
                .filter(t->!t.equals(noteEntity.getTitle()))
                .collect(Collectors.toList());
        randomizer.shuffle(list);
        List<String> selectedList = list.stream().limit(5).collect(Collectors.toList());
        selectedList.add(noteEntity.getTitle());
        Collections.shuffle(selectedList);

        return selectedList;

    }

    public AnswerEntity buildAnswer() {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setReviewPointEntity(reviewPointEntity);
        return answerEntity;
    }

    private TreeNodeModel getTreeNodeModel() {
        return modelFactoryService.toTreeNodeModel(noteEntity);
    }
}
