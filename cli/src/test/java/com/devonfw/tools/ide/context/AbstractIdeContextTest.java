package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;

import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;

/**
 * Abstract base class for tests that need mocked instances of {@link IdeContext}.
 */
public abstract class AbstractIdeContextTest extends Assertions {

  /** {@link #newContext(String) Name of test project} {@value}. */
  protected static final String PROJECT_BASIC = "basic";

  /** The source {@link Path} to the test projects. */
  protected static final Path PATH_PROJECTS = Path.of("src/test/resources/ide-projects");

  // will not use eclipse-target like done in maven via eclipse profile...
  private static final Path PATH_PROJECTS_COPY = Path.of("target/test-projects/");

  /** Chunk size to use for progress bars **/
  private static final int CHUNK_SIZE = 1024;

  /**
   * @param projectTestCaseName the (folder)name of the project test case, in this folder a 'project' folder represents
   *        the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected IdeTestContext newContext(String projectTestCaseName) {

    return newContext(projectTestCaseName, null, true);
  }

  /**
   * @param projectTestCaseName the (folder)name of the project test case, in this folder a 'project' folder represents
   *        the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String projectTestCaseName, String projectPath) {

    return newContext(projectTestCaseName, projectPath, true);
  }

  /**
   * @param projectTestCaseName the (folder)name of the project test case, in this folder a 'project' folder represents
   *        the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @param copyForMutation - {@code true} to create a copy of the project that can be modified by the test,
   *        {@code false} otherwise (only to save resources if you are 100% sure that your test never modifies anything
   *        in that project.)
   * @param answers the answers to use for the {@link IdeTestContext}.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String projectTestCaseName, String projectPath, boolean copyForMutation,
      String... answers) {

    Path sourceDir = PATH_PROJECTS.resolve(projectTestCaseName);
    Path userDir = sourceDir.resolve("project");
    IdeTestContext context;
    if (copyForMutation) {
      Path projectDir = PATH_PROJECTS_COPY.resolve(projectTestCaseName);
      FileAccess fileAccess = new FileAccessImpl(IdeTestContextMock.get());
      fileAccess.delete(projectDir);
      fileAccess.mkdirs(PATH_PROJECTS_COPY);
      fileAccess.copy(sourceDir, projectDir, FileCopyMode.COPY_TREE_OVERRIDE_TREE);
      userDir = projectDir.resolve("project");
    }
    if (projectPath != null) {
      userDir = userDir.resolve(projectPath);
    }
    context = new IdeTestContext(userDir, answers);
    return context;
  }

  /**
   * @param projectPath the relative path inside the test project where to create the context.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(Path projectPath) {

    return new IdeTestContext(projectPath);
  }

  /**
   * @param projectPath the relative path inside the test project where to create the context.
   * @param errors list of error messages.
   * @param outs list of out messages.
   * @param exitCode the exit code.
   * @param isOnline boolean if it should be run in online mode.
   * @return the {@link GitContextTestContext} pointing to that project.
   */
  protected static GitContextTestContext newGitContext(Path projectPath, List<String> errors, List<String> outs,
      int exitCode, boolean isOnline) {

    GitContextTestContext context;
    context = new GitContextTestContext(isOnline, projectPath);
    context.setErrors(errors);
    context.setOuts(outs);
    context.setExitCode(exitCode);
    return context;
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param level the expected {@link IdeLogLevel}.
   * @param message the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   */
  protected static void assertLogMessage(IdeTestContext context, IdeLogLevel level, String message) {

    assertLogMessage(context, level, message, false);
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param level the expected {@link IdeLogLevel}.
   * @param message the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   * @param contains - {@code true} if the given {@code message} may only be a sub-string of the log-message to assert,
   *        {@code false} otherwise (the entire log message including potential parameters being filled in is asserted).
   */
  protected static void assertLogMessage(IdeTestContext context, IdeLogLevel level, String message, boolean contains) {

    IdeTestLogger logger = context.level(level);
    ListAssert<String> assertion = assertThat(logger.getMessages()).as(level.name() + "-Log messages");
    if (contains) {
      Condition<String> condition = new Condition<>() {
        public boolean matches(String e) {

          return e.contains(message);
        }
      };
      assertion.filteredOn(condition).isNotEmpty();
    } else {
      assertion.contains(message);
    }
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default
   * behavior
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   * @param chunkSize size of the chunk.
   * @param chunkCount amount of chunks.
   * @param restSize remaining size.
   */
  protected static void assertProgressBar(IdeContext context, String taskName, long maxSize, long chunkSize,
      int chunkCount, long restSize) {

    AbstractIdeTestContext testContext = (AbstractIdeTestContext) context;
    IdeProgressBarTestImpl progressBar = testContext.getProgressBarMap().get(taskName);
    assertThat(progressBar).as(taskName).isNotNull();
    assertThat(progressBar.getMaxSize()).isEqualTo(maxSize);
    List<IdeProgressBarTestImpl.ProgressEvent> eventList = progressBar.getEventList();
    assertThat(eventList).hasSize(chunkCount + 1);
    for (int i = 0; i <= chunkCount; i++) {
      IdeProgressBarTestImpl.ProgressEvent progressEvent = eventList.get(i);
      long stepSize = chunkSize;
      if (i == chunkCount) {
        stepSize = restSize;
      }
      assertThat(progressEvent.getStepSize()).isEqualTo(stepSize);
    }
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default
   * behaviour, chunk size is fixed.
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   * @param chunkCount amount of chunks.
   * @param restSize remaining size.
   */
  protected static void assertProgressBar(IdeContext context, String taskName, long maxSize, int chunkCount,
      long restSize) {

    assertProgressBar(context, taskName, maxSize, CHUNK_SIZE, chunkCount, restSize);
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default
   * behaviour, chunk size is fixed, chunk count and rest size get automatically calculated.
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   */
  protected static void assertProgressBar(IdeContext context, String taskName, long maxSize) {

    int chunkCount = (int) (maxSize / CHUNK_SIZE);
    long restSize = maxSize % CHUNK_SIZE;
    assertProgressBar(context, taskName, maxSize, CHUNK_SIZE, chunkCount, restSize);
  }

}
