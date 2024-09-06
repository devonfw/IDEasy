package com.devonfw.tools.ide.io;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.SystemInfoMock;

/**
 * Test of {@link IdeProgressBarConsole}.
 */
public class IdeProgressBarConsoleTest extends AbstractIdeContextTest {

  @Test
  public void testProgressBarMaxSizeUnknownStepBy(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", -1);

    // act
    progressBarConsole.stepBy(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
  }

  @Test
  public void testProgressBarMaxSizeUnknownDoStepTo(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long stepSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", -1);

    // act
    progressBarConsole.doStepTo(stepSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(true);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(stepSize);
  }


  @Test
  public void testProgressBarMaxSizeKnownStepBy(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", maxSize);

    //act
    progressBarConsole.stepBy(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownDoStepTo(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", maxSize);

    //act
    progressBarConsole.doStepTo(maxSize);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().isIndefinite()).isEqualTo(false);
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxSizeKnownIncompleteSteps(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", maxSize);

    // act
    progressBarConsole.stepBy(1L);
    progressBarConsole.close();

    // assert
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarMaxOverflow(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", maxSize);
    // act
    progressBarConsole.stepBy(20000L);
    progressBarConsole.close();

    //assert
    assertThat(progressBarConsole.getProgressBar().getMax()).isEqualTo(maxSize);
  }

  @Test
  public void testProgressBarStyleSwitchOnNonWindows(@TempDir Path tempDir) throws Exception {
    // arrange
    IdeTestContext context = newContext(tempDir);
    SystemInfo systemInfo = SystemInfoMock.of("linux");
    context.setSystemInfo(systemInfo);
    long maxSize = 10000L;
    IdeProgressBarConsole progressBarConsole = new IdeProgressBarConsole(context.getSystemInfo(), "downloading", maxSize);
    // act
    progressBarConsole.close();
  }
}
