package com.devonfw.tools.ide.step;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StepSummaryTest extends Assertions {

  @Test
  void testInitialValues() {
    StepSummary summary = new StepSummary();
    assertThat(summary.getTotal()).isZero();
    assertThat(summary.getError()).isZero();
    assertThat(summary.toString()).isEqualTo("0 step(s) failed out of 0 steps.");
  }

  @Test
  void testAddSuccess() {
    StepSummary summary = new StepSummary();
    summary.add(false);
    assertThat(summary.getTotal()).isEqualTo(1);
    assertThat(summary.getError()).isZero();
    assertThat(summary.toString()).isEqualTo("0 step(s) failed out of 1 steps.");
  }

  @Test
  void testAddFailure() {
    StepSummary summary = new StepSummary();
    summary.add(true);
    assertThat(summary.getTotal()).isEqualTo(1);
    assertThat(summary.getError()).isEqualTo(1);
    assertThat(summary.toString()).isEqualTo("1 step(s) failed out of 1 steps.");
  }

  @Test
  void testAddMixed() {
    StepSummary summary = new StepSummary();
    summary.add(false);
    summary.add(true);
    summary.add(false);
    summary.add(true);
    assertThat(summary.getTotal()).isEqualTo(4);
    assertThat(summary.getError()).isEqualTo(2);
    assertThat(summary.toString()).isEqualTo("2 step(s) failed out of 4 steps.");
  }
}
