package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.controllers.dto.ReviewStatus;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.MemoryTrackerModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/recalls")
class RestRecallsController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestRecallsController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/overview")
  @Transactional(readOnly = true)
  public ReviewStatus overview(@RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(currentUser, currentUTCTimestamp, timeZone, modelFactoryService)
        .getReviewStatus();
  }

  @GetMapping("/initial")
  @Transactional(readOnly = true)
  public List<Note> initialReview(@RequestParam(value = "timezone") String timezone) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return new RecallService(currentUser, currentUTCTimestamp, timeZone, modelFactoryService)
        .getDueInitialMemoryTrackers()
        .toList();
  }

  @PostMapping(path = "")
  @Transactional
  public MemoryTracker create(@RequestBody InitialInfo initialInfo) {
    currentUser.assertLoggedIn();
    MemoryTracker memoryTracker =
        MemoryTracker.buildMemoryTrackerForNote(
            modelFactoryService.entityManager.find(Note.class, initialInfo.noteId));
    memoryTracker.setRemovedFromTracking(initialInfo.skipMemoryTracking);

    MemoryTrackerModel memoryTrackerModel = modelFactoryService.toMemoryTrackerModel(memoryTracker);
    memoryTrackerModel.initialReview(
        testabilitySettings.getCurrentUTCTimestamp(), currentUser.getEntity());
    return memoryTrackerModel.getEntity();
  }

  @GetMapping(value = {"/repeat"})
  @Transactional
  public DueMemoryTrackers repeatReview(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    currentUser.assertLoggedIn();
    ZoneId timeZone = ZoneId.of(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return new RecallService(currentUser, currentUTCTimestamp, timeZone, modelFactoryService)
        .getDueMemoryTrackers(dueInDays == null ? 0 : dueInDays);
  }
}
