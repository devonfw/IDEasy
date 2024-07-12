package com.devonfw.tools.ide.process;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.ReflectionUtils;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Unit tests of {@link ProcessContextImpl}.
 */
public class ProcessContextImplTest extends AbstractIdeContextTest {

  private ProcessContextImpl processConttextUnderTest;

  private Process processMock;

  private ProcessBuilder mockProcessBuilder;

  private IdeContext context;

  @BeforeEach
  public void setUp() throws Exception {

    this.mockProcessBuilder = mock(ProcessBuilder.class);
    this.context = newContext(PROJECT_BASIC, null, false);
    this.processConttextUnderTest = new ProcessContextImpl(this.context);

    Field field = ReflectionUtils.findFields(ProcessContextImpl.class, f -> f.getName().equals("processBuilder"),
        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);

    field.setAccessible(true);
    field.set(this.processConttextUnderTest, this.mockProcessBuilder);
    field.setAccessible(false);

    // underTest needs executable
    Field underTestExecutable = ReflectionUtils.findFields(ProcessContextImpl.class,
        f -> f.getName().equals("executable"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    underTestExecutable.setAccessible(true);
    underTestExecutable.set(this.processConttextUnderTest,
        TEST_PROJECTS.resolve("_ide/software/nonExistingBinaryForTesting"));
    underTestExecutable.setAccessible(false);

    this.processMock = mock(Process.class);
    when(this.mockProcessBuilder.start()).thenReturn(this.processMock);

    when(this.mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)).thenReturn(this.mockProcessBuilder);
    when(this.mockProcessBuilder.redirectError(ProcessBuilder.Redirect.PIPE)).thenReturn(this.mockProcessBuilder);
    when(this.mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD)).thenReturn(this.mockProcessBuilder);
    when(this.mockProcessBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)).thenReturn(this.mockProcessBuilder);
    when(this.mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)).thenReturn(this.mockProcessBuilder);
    when(this.mockProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)).thenReturn(this.mockProcessBuilder);

  }

  @Test
  public void missingExecutableShouldThrowIllegalState() throws Exception {

    // arrange
    String expectedMessage = "Missing executable to run process!";

    Field underTestExecutable = ReflectionUtils.findFields(ProcessContextImpl.class,
        f -> f.getName().equals("executable"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    underTestExecutable.setAccessible(true);
    underTestExecutable.set(this.processConttextUnderTest, null);
    underTestExecutable.setAccessible(false);

    // act & assert
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      this.processConttextUnderTest.run(ProcessMode.DEFAULT);
    });

    String actualMessage = exception.getMessage();

    assertThat(actualMessage).isEqualTo(expectedMessage);
  }

  @Test
  public void onSuccessfulProcessStartReturnSuccessResult() throws Exception {

    // arrange

    when(this.processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = this.processConttextUnderTest.run(ProcessMode.DEFAULT);

    // assert
    verify(this.mockProcessBuilder).redirectOutput(
        (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));

    verify(this.mockProcessBuilder).redirectError(
        (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));
    assertThat(result.isSuccessful()).isTrue();

  }

  @Test
  public void enablingCaptureShouldRedirectAndCaptureStreamsCorrectly() throws Exception {

    // arrange
    when(this.processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);
    String outputText = "hello world";
    String errorText = "error";

    try (InputStream outputStream = new ByteArrayInputStream(outputText.getBytes());
        InputStream errorStream = new ByteArrayInputStream(errorText.getBytes())) {

      when(this.processMock.getInputStream()).thenReturn(outputStream);

      when(this.processMock.getErrorStream()).thenReturn(errorStream);

      // act
      ProcessResult result = this.processConttextUnderTest.run(ProcessMode.DEFAULT_CAPTURE);

      // assert
      verify(this.mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.PIPE)));

      verify(this.mockProcessBuilder).redirectError(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.PIPE)));

      assertThat(outputText).isEqualTo(result.getOut().get(0));
      assertThat(errorText).isEqualTo(result.getErr().get(0));
    }
  }

  @ParameterizedTest
  @EnumSource(value = ProcessMode.class, names = { "BACKGROUND", "BACKGROUND_SILENT" })
  public void enablingBackgroundProcessShouldNotBeAwaitedAndShouldNotPassStreams(ProcessMode processMode)
      throws Exception {

    // arrange
    when(this.processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = this.processConttextUnderTest.run(processMode);

    // assert
    if (processMode == ProcessMode.BACKGROUND) {
      verify(this.mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));

      verify(this.mockProcessBuilder).redirectError(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));
    } else if (processMode == ProcessMode.BACKGROUND_SILENT) {
      verify(this.mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));

      verify(this.mockProcessBuilder).redirectError(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));
    }

    verify(this.processMock, never()).waitFor();

    assertThat(result.getOut()).isNull();
    assertThat(result.getErr()).isNull();

  }

  @Test
  public void unsuccessfulProcessShouldThrowIllegalState() throws Exception {

    // arrange
    when(this.processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);

    this.processConttextUnderTest.errorHandling(ProcessErrorHandling.THROW);

    // act & assert
    assertThrows(IllegalStateException.class, () -> {
      this.processConttextUnderTest.run(ProcessMode.DEFAULT);
    });

  }

  @ParameterizedTest
  @EnumSource(value = ProcessErrorHandling.class, names = { "WARNING", "ERROR" })
  public void ProcessWarningAndErrorShouldBeLogged(ProcessErrorHandling processErrorHandling) throws Exception {

    // arrange
    when(this.processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);
    this.processConttextUnderTest.errorHandling(processErrorHandling);
    String expectedMessage = "failed with exit code 4!";
    // act
    this.processConttextUnderTest.run(ProcessMode.DEFAULT);

    // assert
    IdeLogLevel level = convertToIdeLogLevel(processErrorHandling);
    assertLogMessage((IdeTestContext) this.context, level, expectedMessage, true);
  }

  private IdeLogLevel convertToIdeLogLevel(ProcessErrorHandling processErrorHandling) {

    return switch (processErrorHandling) {
      case NONE, THROW -> null;
      case WARNING -> IdeLogLevel.WARNING;
      case ERROR -> IdeLogLevel.ERROR;
    };
  }

}
