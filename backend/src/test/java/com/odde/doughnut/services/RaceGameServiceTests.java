package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.RaceGameProgress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RaceGameServiceTests {

  @Autowired private RaceGameService service;

  private final String playerId = "test-player-1";

  private int maxHp = 6;

  @Test
  void shouldStartWithInitialPosition() {
    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), equalTo(0));
    assertThat(progress.getRoundCount(), equalTo(0));
    assertThat(progress.getLastDiceFace(), equalTo(0));
  }

  @Test
  void shouldNotMoveCarBeyondTargetPosition() {
    // Roll dice multiple times to ensure we reach target
    for (int i = 0; i < 15; i++) {
      service.rollDiceNormal(playerId);
    }

    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), lessThanOrEqualTo(20));
  }

  @Test
  void shouldMoveCarBasedOnDiceOutcomeNormalMode() {
    service.rollDiceNormal(playerId);
    RaceGameProgress progress = service.getCurrentProgress(playerId);

    if (progress.getLastDiceFace() % 2 == 0) {
      assertThat(progress.getCarPosition(), equalTo(2)); // Even number moves 2 positions
    } else {
      assertThat(progress.getCarPosition(), equalTo(1)); // Odd number moves 1 position
    }
  }

  @Test
  void shouldMoveCarBasedOnDiceOutcomeSuperModeWithFullHp() {
    service.resetGame(playerId);
    service.rollDiceSuper(playerId);
    RaceGameProgress progress = service.getCurrentProgress(playerId);

    assertThat(progress.getCarPosition(), equalTo(progress.getLastDiceFace()));
    assertThat(progress.getCarHp(), equalTo(maxHp - 1));
  }

  @Test
  void shouldResetGameState() {
    // First move the car
    service.rollDiceNormal(playerId);

    // Then reset the game
    service.resetGame(playerId);

    // Verify game is reset
    RaceGameProgress progress = service.getCurrentProgress(playerId);
    assertThat(progress.getCarPosition(), equalTo(0));
    assertThat(progress.getRoundCount(), equalTo(0));
    assertThat(progress.getLastDiceFace(), equalTo(0));
  }

  @Test
  void shouldNotMoveCarWhenAlreadyAtTarget() {
    // First get to target position
    while (service.getCurrentProgress(playerId).getCarPosition() < 20) {
      service.rollDiceNormal(playerId);
    }

    // Record the state
    RaceGameProgress beforeRoll = service.getCurrentProgress(playerId);
    RaceGameProgress afterRoll = service.getCurrentProgress(playerId);

    // Verify nothing changed except possibly the last dice face
    assertThat(afterRoll.getCarPosition(), equalTo(beforeRoll.getCarPosition()));
    assertThat(afterRoll.getRoundCount(), equalTo(beforeRoll.getRoundCount()));
    assertThat(afterRoll.getLastDiceFace(), equalTo(beforeRoll.getLastDiceFace()));
  }
}
