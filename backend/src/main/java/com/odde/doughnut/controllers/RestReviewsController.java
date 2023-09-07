package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.DueReviewPoints;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.MarkedQuestionRequest;
import com.odde.doughnut.entities.json.ReviewStatus;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.Reviewing;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.MarkedQuestionService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reviews")
class RestReviewsController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestReviewsController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ReviewStatus overview() {
    currentUser.assertLoggedIn();
    return currentUser
        .createReviewing(testabilitySettings.getCurrentUTCTimestamp())
        .getReviewStatus();
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<ReviewPoint> initialReview() {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());

    return reviewing.getDueInitialReviewPoints().collect(Collectors.toList());
  }

  @PostMapping(path = "")
  @Transactional
  public ReviewPoint create(@RequestBody InitialInfo initialInfo) {
    currentUser.assertLoggedIn();
    ReviewPoint reviewPoint =
        ReviewPoint.buildReviewPointForThing(
            modelFactoryService.thingRepository.findById(initialInfo.thingId).orElse(null));
    reviewPoint.setRemovedFromReview(initialInfo.skipReview);

    ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
    reviewPointModel.initialReview(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return reviewPointModel.getEntity();
  }

  @GetMapping(value = {"/repeat"})
  @Transactional
  public DueReviewPoints repeatReview(
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    currentUser.assertLoggedIn();
    Reviewing reviewing = currentUser.createReviewing(testabilitySettings.getCurrentUTCTimestamp());
    return reviewing.getDueReviewPoints(dueInDays, testabilitySettings.getRandomizer());
  }

  @GetMapping(path = "/answers/{answer}")
  @Transactional
  public AnsweredQuestion showAnswer(@PathVariable("answer") Answer answer)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(answer);
    return modelFactoryService.toAnswerModel(answer).getAnswerViewedByUser(currentUser.getEntity());
  }

  private MarkedQuestionService getMarkedQuestionService(User user) {
    return new MarkedQuestionService(
        user, testabilitySettings.getCurrentUTCTimestamp(), modelFactoryService);
  }

  @PostMapping(path = "/mark_question")
  @Transactional
  public Integer markQuestion(@RequestBody MarkedQuestionRequest markedQuestionRequest) {
    MarkedQuestion markedQuestion =
        getMarkedQuestionService(currentUser.getEntity()).markQuestion(markedQuestionRequest);
    if (!markedQuestionRequest.isGood && !StringUtils.hasText(markedQuestionRequest.comment)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Comments can not be empty to mark a bad question.");
    }
    return markedQuestion.getId();
  }

  @DeleteMapping(value = "/mark_question/{id}")
  public Integer deleteMarkQuestion(@PathVariable Integer id) {
    modelFactoryService.markedQuestionRepository.deleteById(id);
    return id;
  }

  @GetMapping(path = "/all_marked_questions")
  public List<MarkedQuestion> getAllMarkedQuestions() {
    Iterable<MarkedQuestion> questions = modelFactoryService.markedQuestionRepository.findAll();
    List<MarkedQuestion> result = new ArrayList<>();
    for (MarkedQuestion markedQuestion : questions) {
      result.add(markedQuestion);
    }
    return result;
  }
}
