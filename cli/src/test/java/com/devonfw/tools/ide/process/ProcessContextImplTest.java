package com.devonfw.tools.ide.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

public class ProcessContextImplTest extends AbstractIdeContextTest {

  private ProcessContextImpl underTest;

  private Process processMock;

  private ProcessBuilder mockProcessBuilder;

  private IdeContext context;

  private final String projectPath = "workspaces/foo-test/my-git-repo";

  @BeforeEach
  public void setUp() throws Exception {

    mockProcessBuilder = mock(ProcessBuilder.class);
    context = newContext("basic", projectPath, false);
    underTest = new ProcessContextImpl(context);

    Field field = ReflectionUtils.findFields(ProcessContextImpl.class, f -> f.getName().equals("processBuilder"),
        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);

    field.setAccessible(true);
    field.set(underTest, mockProcessBuilder);
    field.setAccessible(false);

    // underTest needs executable
    Field underTestExecutable = ReflectionUtils.findFields(ProcessContextImpl.class,
        f -> f.getName().equals("executable"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    underTestExecutable.setAccessible(true);
    underTestExecutable.set(underTest, PATH_PROJECTS.resolve("_ide/software/nonExistingBinaryForTesting"));
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
  public void streamCapturingAndBackgroundProcessShouldThrowIllegalState() {

    // arrange
    String expectedMessage = "It is not possible for the main process to capture the streams of the subprocess (background process) !";

    // act & assert
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      underTest.run(true, true);
    });

    String actualMessage = exception.getMessage();

    assertEquals(actualMessage, expectedMessage);
  }

  @Test
  public void missingExecutableShouldThrowIllegalState() {

    // arrange
    String expectedMessage = "Missing executable to run process!";

    // act & assert
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      underTest.run(false, false);
    });

    String actualMessage = exception.getMessage();

    assertEquals(actualMessage, expectedMessage);
  }

  @Test
  public void onSuccessfulProcessStartReturnSuccessResult() throws Exception {

    // arrange

    when(processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = underTest.run(false, false);

    // assert
    assertTrue(result.isSuccessful());

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
      ProcessResult result = underTest.run(true, false);

      // assert
      verify(mockProcessBuilder)
          .redirectOutput((ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.PIPE)));

      verify(mockProcessBuilder)
          .redirectError((ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.PIPE)));

      assertEquals(outputText, result.getOut().get(0));
      assertEquals(errorText, result.getErr().get(0));
    }
  }

  @Test
  public void enablingBackgroundProcessShouldNotBeAwaitedAndShouldNotPassStreams() throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.SUCCESS);

    // act
    ProcessResult result = underTest.run(false, true);

    // assert
    verify(mockProcessBuilder)
        .redirectOutput((ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));

    verify(mockProcessBuilder)
        .redirectError((ProcessBuilder.Redirect) argThat(arg -> arg.equals(ProcessBuilder.Redirect.DISCARD)));

    verify(processMock, never()).waitFor();

    assertNull(result.getOut());
    assertNull(result.getErr());

  }

  @Test
  public void unsuccessfulProcessShouldThrowIllegalState() throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);

    underTest.errorHandling(ProcessErrorHandling.THROW);

    // act & assert
    assertThrows(IllegalStateException.class, () -> {
      underTest.run(false, false);
    });

  }

  @ParameterizedTest
  @EnumSource(value = ProcessErrorHandling.class, names = { "WARNING", "ERROR" })
  public void ProcessWarningAndErrorShouldBeLogged(ProcessErrorHandling processErrorHandling) throws Exception {

    // arrange
    when(processMock.waitFor()).thenReturn(ProcessResult.TOOL_NOT_INSTALLED);
    underTest.errorHandling(processErrorHandling);
    String expectedMessage = "failed with exit code ";
    // act
    underTest.run(false, false);

    // assert
    IdeLogLevel level = convertToIdeLogLevel(processErrorHandling);
    assertLogMessage((IdeTestContext) context, level, expectedMessage);
  }

  private IdeLogLevel convertToIdeLogLevel(ProcessErrorHandling processErrorHandling) {

    return switch (processErrorHandling) {
      case NONE, THROW -> null;
      case WARNING -> IdeLogLevel.WARNING;
      case ERROR -> IdeLogLevel.ERROR;
    };
  }

}
