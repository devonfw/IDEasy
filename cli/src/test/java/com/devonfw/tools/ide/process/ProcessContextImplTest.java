package com.devonfw.tools.ide.process;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    mockProcessBuilder = mock(ProcessBuilder.class);
    context = newContext(PROJECT_BASIC, null, false);
    processConttextUnderTest = new ProcessContextImpl(context);

    Field field = ReflectionUtils.findFields(ProcessContextImpl.class, f -> f.getName().equals("processBuilder"),
        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);

    field.setAccessible(true);
    field.set(processConttextUnderTest, mockProcessBuilder);
    field.setAccessible(false);

    // underTest needs executable
    Field underTestExecutable = ReflectionUtils.findFields(ProcessContextImpl.class,
        f -> f.getName().equals("executable"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    underTestExecutable.setAccessible(true);
    underTestExecutable.set(processConttextUnderTest,
        TEST_PROJECTS.resolve("_ide/software/nonExistingBinaryForTesting"));
    underTestExecutable.setAccessible(false);

    processMock = mock(Process.class);
    when(mockProcessBuilder.start()).thenReturn(processMock);

    when(mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)).thenReturn(mockProcessBuilder);
    when(mockProcessBuilder.redirectError(ProcessBuilder.Redirect.PIPE)).thenReturn(mockProcessBuilder);
    when(mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD)).thenReturn(mockProcessBuilder);
    when(mockProcessBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)).thenReturn(mockProcessBuilder);
    when(mockProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)).thenReturn(mockProcessBuilder);
    when(mockProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)).thenReturn(mockProcessBuilder);

  }

  @Test
  public void missingExecutableShouldThrowIllegalState() throws Exception {

    // arrange
    String expectedMessage = "Missing executable to run process!";

    Field underTestExecutable = ReflectionUtils.findFields(ProcessContextImpl.class,
        f -> f.getName().equals("executable"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    underTestExecutable.setAccessible(true);
    underTestExecutable.set(processConttextUnderTest, null);
    underTestExecutable.setAccessible(false);

    // act & assert
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      processConttextUnderTest.run(ProcessMode.DEFAULT);
    });

    String actualMessage = exception.getMessage();

    assertThat(actualMessage).isEqualTo(expectedMessage);
  }

  @Test
  public void onSuccessfulProcessStartReturnSuccessResult() throws Exception {

    // arrange

    when(processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = processConttextUnderTest.run(ProcessMode.DEFAULT);

    // assert
    verify(mockProcessBuilder).redirectOutput(
        (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));

    verify(mockProcessBuilder).redirectError(
        (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));
    assertThat(result.isSuccessful()).isTrue();

  }

  @Test
  public void enablingCaptureShouldRedirectAndCaptureStreamsCorrectly() throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);
    String outputText = "hello world";
    String errorText = "error";

    try (InputStream outputStream = new ByteArrayInputStream(outputText.getBytes());
        InputStream errorStream = new ByteArrayInputStream(errorText.getBytes())) {

      when(processMock.getInputStream()).thenReturn(outputStream);

      when(processMock.getErrorStream()).thenReturn(errorStream);

      // act
      ProcessResult result = processConttextUnderTest.run(ProcessMode.DEFAULT_CAPTURE);

      // assert
      verify(mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.PIPE)));

      verify(mockProcessBuilder).redirectError(
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
    when(processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = processConttextUnderTest.run(processMode);

    // assert
    if (processMode == ProcessMode.BACKGROUND) {
      verify(mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));

      verify(mockProcessBuilder).redirectError(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.INHERIT)));
    } else if (processMode == ProcessMode.BACKGROUND_SILENT) {
      verify(mockProcessBuilder).redirectOutput(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));

      verify(mockProcessBuilder).redirectError(
          (ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));
    }

    verify(processMock, never()).waitFor();

    assertThat(result.getOut()).isNull();
    assertThat(result.getErr()).isNull();

  }

  @Test
  public void unsuccessfulProcessShouldThrowIllegalState() throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);

    processConttextUnderTest.errorHandling(ProcessErrorHandling.THROW);

    // act & assert
    assertThrows(IllegalStateException.class, () -> {
      processConttextUnderTest.run(ProcessMode.DEFAULT);
    });

  }

  @ParameterizedTest
  @EnumSource(value = ProcessErrorHandling.class, names = { "WARNING", "ERROR" })
  public void ProcessWarningAndErrorShouldBeLogged(ProcessErrorHandling processErrorHandling) throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);
    processConttextUnderTest.errorHandling(processErrorHandling);
    String expectedMessage = "failed with exit code 4!";
    // act
    processConttextUnderTest.run(ProcessMode.DEFAULT);

    // assert
    IdeLogLevel level = convertToIdeLogLevel(processErrorHandling);
    assertLogMessage((IdeTestContext) context, level, expectedMessage, true);
  }

  private IdeLogLevel convertToIdeLogLevel(ProcessErrorHandling processErrorHandling) {

    return switch (processErrorHandling) {
      case NONE, THROW -> null;
      case WARNING -> IdeLogLevel.WARNING;
      case ERROR -> IdeLogLevel.ERROR;
    };
  }

}
