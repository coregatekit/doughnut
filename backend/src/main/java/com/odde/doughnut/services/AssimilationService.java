package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.ReviewScope;
import com.odde.doughnut.models.SubscriptionModel;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class AssimilationService {
  private final UserModel userModel;
  private final ModelFactoryService modelFactoryService;
  private final Timestamp currentUTCTimestamp;
  private final ZoneId timeZone;

  public AssimilationService(
      UserModel user,
      ModelFactoryService modelFactoryService,
      Timestamp currentUTCTimestamp,
      ZoneId timeZone) {
    userModel = user;
    this.modelFactoryService = modelFactoryService;
    this.currentUTCTimestamp = currentUTCTimestamp;
    this.timeZone = timeZone;
  }

  private Stream<Note> getDueNewMemoryTracker(ReviewScope reviewScope, int count) {
    return reviewScope.getThingHaveNotBeenReviewedAtAll().limit(count);
  }

  private int unassimilatedCount() {
    Integer subscribedCount =
        getSubscriptionModelStream()
            .map(this::getPendingNewMemoryTrackerCount)
            .reduce(Integer::sum)
            .orElse(0);
    return subscribedCount + getPendingNewMemoryTrackerCount(userModel);
  }

  private int getPendingNewMemoryTrackerCount(ReviewScope reviewScope) {
    return reviewScope.getThingsHaveNotBeenReviewedAtAllCount();
  }

  private Stream<SubscriptionModel> getSubscriptionModelStream() {
    return userModel.getEntity().getSubscriptions().stream()
        .map(modelFactoryService::toSubscriptionModel);
  }

  public Stream<Note> getDueInitialMemoryTrackers() {
    int count = remainingDailyNewNotesCount();
    if (count <= 0) {
      return Stream.empty();
    }
    List<Integer> alreadyInitialReviewed =
        getNewMemoryTrackersOfToday().stream()
            .map(MemoryTracker::getNote)
            .map(Note::getId)
            .toList();
    return Stream.concat(
            getSubscriptionModelStream()
                .flatMap(
                    sub ->
                        getDueNewMemoryTracker(
                            sub, sub.needToLearnCountToday(alreadyInitialReviewed))),
            getDueNewMemoryTracker(userModel, count))
        .limit(count);
  }

  private int remainingDailyNewNotesCount() {
    long sameDayCount = getAssimilatedCountOfTheDay();
    return (int) (userModel.getEntity().getDailyNewNotesCount() - sameDayCount);
  }

  private int getAssimilatedCountOfTheDay() {
    return getNewMemoryTrackersOfToday().size();
  }

  private List<MemoryTracker> getNewMemoryTrackersOfToday() {
    Timestamp oneDayAgo = TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, -24);
    return userModel.getRecentMemoryTrackers(oneDayAgo).stream()
        .filter(p -> p.isInitialReviewOnSameDay(currentUTCTimestamp, timeZone))
        .filter(p -> !p.getRemovedFromTracking())
        .toList();
  }

  public AssimilationCountDTO getCounts() {
    int totalUnassimilatedCount = unassimilatedCount();
    int assimilatedCountOfTheDay = getAssimilatedCountOfTheDay();
    int dueCount = 0;
    if (getDueInitialMemoryTrackers().findFirst().isPresent()) {
      dueCount =
          Math.min(
              (userModel.getEntity().getDailyNewNotesCount() - assimilatedCountOfTheDay),
              totalUnassimilatedCount);
    }
    return new AssimilationCountDTO(dueCount, assimilatedCountOfTheDay, totalUnassimilatedCount);
  }
}
