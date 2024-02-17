package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.json.QuestionSuggestionParams;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class SuggestedQuestionForFineTuningModel {
  private final SuggestedQuestionForFineTuning entity;
  private final ModelFactoryService modelFactoryService;

  public SuggestedQuestionForFineTuningModel(
      SuggestedQuestionForFineTuning suggestion, ModelFactoryService modelFactoryService) {
    this.entity = suggestion;
    this.modelFactoryService = modelFactoryService;
  }

  public SuggestedQuestionForFineTuning update(QuestionSuggestionParams params) {
    entity.preserveQuestion(params.preservedQuestion);
    entity.setPreservedNoteContent(params.preservedNoteContent);
    entity.setComment(params.comment);
    entity.setPositiveFeedback(params.positiveFeedback);
    entity.setRealCorrectAnswers(params.realCorrectAnswers);
    return modelFactoryService.save(entity);
  }

  public SuggestedQuestionForFineTuning suggestQuestionForFineTuning(
      QuizQuestionAIQuestion quizQuestion,
      QuestionSuggestionCreationParams suggestionCreationParams,
      User user,
      Timestamp currentUTCTimestamp) {
    entity.setUser(user);
    entity.setCreatedAt(currentUTCTimestamp);
    entity.preserveNoteContent(quizQuestion.getNote());
    entity.preserveQuestion(quizQuestion.getMcqWithAnswer());
    entity.setComment(suggestionCreationParams.comment);
    entity.setPositiveFeedback(suggestionCreationParams.isPositiveFeedback);
    if (suggestionCreationParams.isPositiveFeedback) {
      entity.setRealCorrectAnswers("%d".formatted(quizQuestion.getCorrectAnswerIndex1()));
    }
    return modelFactoryService.save(entity);
  }

  public SuggestedQuestionForFineTuning duplicate() {
    SuggestedQuestionForFineTuning newObject = new SuggestedQuestionForFineTuning();
    newObject.setUser(entity.getUser());
    newObject.preserveQuestion(entity.getPreservedQuestion());
    newObject.setPreservedNoteContent(entity.getPreservedNoteContent());
    newObject.setComment(entity.getComment());
    newObject.setPositiveFeedback(entity.isPositiveFeedback());
    return modelFactoryService.save(newObject);
  }

  public SuggestedQuestionForFineTuning delete() {
    return modelFactoryService.remove(entity);
  }
}
