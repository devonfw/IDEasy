package com.devonfw.tools.ide.io;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link IdeProgressBarConsole}.
 */
public class IdeProgressBarConsoleTest extends AbstractIdeContextTest {

  @Test
  public void testProgressBarMaxSizeUnknownStepBy() throws Exception {
    // arrange
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", -1);

    // act
    progressBarConsole.stepBy(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
  }

  @Test
  public void testProgressBarMaxSizeUnknownDoStepTo() throws Exception {
    // arrange
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", -1);

    // act
    progressBarConsole.doStepTo(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
  }


  @Test
  public void testProgressBarMaxSizeKnownStepBy() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", maxSize);

    //act
    progressBarConsole.stepBy(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownDoStepTo() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", maxSize);

    //act
    progressBarConsole.doStepTo(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownIncompleteSteps() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", maxSize);

    // act
    progressBarConsole.stepBy(1L);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxOverflow() throws Exception {
    // arrange
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(SystemInfoImpl.INSTANCE, "downloading", maxSize);
    // act
    progressBarConsole.stepBy(20000L);
    progressBarConsole.close();

    //assert
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarStyleSwitchOnNonWindows() throws Exception {
    // arrange
    SystemInfo systemInfo = SystemInfoMock.LINUX_X64;
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(systemInfo, "downloading", maxSize);
    // act
    progressBarConsole.close();
  }
}
