package com.devonfw.tools.ide.log;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link IdeLogListenerStringBuilder}.
 */
class IdeLogListenerStringBuilderTest extends Assertions {

  /**
   * Test of the {@link IdeLogListenerStringBuilder#IdeLogListenerStringBuilder() default constructor}. It captures every log event (threshold
   * {@link IdeLogLevel#TRACE}).
   */
  @Test
  void testDefaultConstructorCapturesEveryLogEvent() {

    // arrange
    IdeLogListenerStringBuilder listener = new IdeLogListenerStringBuilder();

    // act & assert (every log event is captured and rejected from the regular pipeline)
    assertThat(listener.onLog(IdeLogEntry.ofTrace("trace message"))).isFalse();
    assertThat(listener.toString()).isEqualTo("trace message\n");

    // the threshold constructor with TRACE behaves the same as the default constructor
    IdeLogListenerStringBuilder traceThreshold = new IdeLogListenerStringBuilder(IdeLogLevel.TRACE);
    assertThat(traceThreshold.onLog(IdeLogEntry.ofTrace("another trace message"))).isFalse();
    assertThat(traceThreshold.toString()).isEqualTo("another trace message\n");
  }

  /**
   * Test of the {@link IdeLogListenerStringBuilder#IdeLogListenerStringBuilder(StringBuilder, IdeLogLevel) thresholding constructor}. Log events below the
   * {@link IdeLogLevel threshold} must be ignored while events of the threshold or a higher level are captured.
   */
  @Test
  void testThresholdOnlyCapturesEnabledLogLevels() {

    // arrange
    StringBuilder builder = new StringBuilder();
    IdeLogListenerStringBuilder listener = new IdeLogListenerStringBuilder(builder, IdeLogLevel.INFO);

    // act & assert (DEBUG is below the threshold and must be ignored, INFO and ERROR are captured)
    assertThat(listener.onLog(IdeLogEntry.ofDebug("debug message"))).isFalse();
    assertThat(builder).isEmpty();
    assertThat(listener.onLog(IdeLogEntry.ofInfo("info message"))).isFalse();
    assertThat(listener.onLog(IdeLogEntry.ofError("error message"))).isFalse();
    assertThat(builder.toString()).isEqualTo("info message\nerror message\n");
  }

}
