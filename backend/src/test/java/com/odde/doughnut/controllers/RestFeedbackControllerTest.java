package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestFeedbackControllerTest {

  @Autowired ConversationService feedbackService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  RestFeedbackController controller;

  @Autowired ModelFactoryService modelFactoryService;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestFeedbackController(currentUser, feedbackService, modelFactoryService);
  }

  @Test
  void testSendFeedbackReturnsOk() {
    FeedbackDTO feedbackDTO = new FeedbackDTO();
    feedbackDTO.setFeedback("This is a feedback");

    Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
    QuizQuestionAndAnswer quizQuestionAndAnswer = new QuizQuestionAndAnswer();
    quizQuestionAndAnswer.setNote(note);

    assertEquals(
        "Feedback received successfully!",
        controller.sendFeedback(feedbackDTO, quizQuestionAndAnswer).getBody());
  }

  @Test
  void testGetFeedbackReturnsZeroConversationsForCurrentUser() {
    List<Conversation> conversations = controller.getFeedback();
    assertEquals(0, conversations.size());
  }

  @Test
  void testGetFeedbackReturnsAllConversationsForCurrentUser() {
    User feedbackGiverUser = makeMe.aUser().please();

    QuizQuestionAndAnswer quizQuestionAndAnswer1 = makeMe.aQuestion().please();
    quizQuestionAndAnswer1.getNote().setCreator(this.currentUser.getEntity());
    Conversation conversation1 = new Conversation();
    conversation1.setConversationInitiator(feedbackGiverUser);
    conversation1.setNoteCreator(quizQuestionAndAnswer1.getNote().getCreator());
    conversation1.setMessage("This is a feedback for the current user");
    conversation1.setQuizQuestionAndAnswer(quizQuestionAndAnswer1);
    makeMe.modelFactoryService.save(conversation1);

    QuizQuestionAndAnswer quizQuestionAndAnswer2 = makeMe.aQuestion().please();
    Conversation conversation2 = new Conversation();
    conversation2.setConversationInitiator(feedbackGiverUser);
    conversation2.setNoteCreator(quizQuestionAndAnswer2.getNote().getCreator());
    conversation2.setMessage("This is a feedback for the other user");
    conversation2.setQuizQuestionAndAnswer(quizQuestionAndAnswer2);
    makeMe.modelFactoryService.save(conversation2);

    List<Conversation> conversations = controller.getFeedback();

    assertEquals(1, conversations.size());
    assertEquals("This is a feedback for the current user", conversations.getFirst().getMessage());
  }
}
