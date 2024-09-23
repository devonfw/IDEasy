package com.devonfw.tools.ide.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;

/**
 * Test of {@link IdeProgressBarConsole}.
 */
// disabled on Windows - see https://github.com/devonfw/IDEasy/issues/618
@DisabledOnOs({ OS.WINDOWS })
public class IdeProgressBarConsoleTest extends AbstractIdeContextTest {

  private IdeProgressBarConsole newProgressBar(long maxSize) {
    SystemInfo systemInfo = SystemInfoImpl.INSTANCE;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(systemInfo, "downloading", maxSize);
    return progressBarConsole;
  }

  @Test
  public void testProgressBarMaxSizeUnknownStepBy() throws Exception {
    // arrange
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(-1);

    // act
    progressBarConsole.stepBy(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(stepSize);
  }

  @Test
  public void testProgressBarMaxSizeUnknownDoStepTo() throws Exception {
    // arrange
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(-1);

    // act
    progressBarConsole.stepBy(100L);
    progressBarConsole.stepTo(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(stepSize);
  }


  @Test
  public void testProgressBarMaxSizeKnownStepBy() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(maxSize);

    //act
    progressBarConsole.stepBy(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getMaxLength()).isEqualTo(maxSize);
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownDoStepTo() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(maxSize);

    //act
    progressBarConsole.doStepTo(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getMaxLength()).isEqualTo(maxSize);
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownIncompleteSteps() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(maxSize);

    // act
    progressBarConsole.stepBy(1L);
    // assert
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(1L);
    // act
    progressBarConsole.close();
    // assert
    assertThat(progressBarConsole.getMaxLength()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxOverflow() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = newProgressBar(maxSize);
    // act
    progressBarConsole.stepBy(20000L);
    progressBarConsole.close();

    //assert
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
    assertThat(progressBarConsole.getCurrentProgress()).isEqualTo(maxSize);
  }

}
