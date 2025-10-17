package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;

import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.repository.ToolRepositoryMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Abstract base class for tests that need mocked instances of {@link IdeContext}.
 */
public abstract class AbstractIdeContextTest extends Assertions {

  private static final Set<String> IDES = Set.of("eclipse", "intellij", "vscode", "android-studio");

  /** {@link #newContext(String) Name of test project} {@value}. */
  protected static final String PROJECT_BASIC = "basic";

  /** Test- */
  protected static final Path TEST_RESOURCES = Path.of("src/test/resources");

  /** The source {@link Path} to the test projects. */
  protected static final Path TEST_PROJECTS = TEST_RESOURCES.resolve("ide-projects");

  // will not use eclipse-target like done in maven via eclipse profile...
  protected static final Path TEST_PROJECTS_COPY = Path.of("target/test-projects");

  /** Chunk size to use for progress bars **/
  private static final int CHUNK_SIZE = 1024;

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected IdeTestContext newContext(String testProject) {

    return newContext(testProject, null, true, null);
  }

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} or {@code null} if not used.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected IdeTestContext newContext(String testProject, WireMockRuntimeInfo wmRuntimeInfo) {

    return newContext(testProject, null, true, wmRuntimeInfo);
  }

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String testProject, String projectPath) {

    return newContext(testProject, projectPath, true, null);
  }

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @param copyForMutation - {@code true} to create a copy of the project that can be modified by the test, {@code false} otherwise (only to save resources
   *     if you are 100% sure that your test never modifies anything in that project.)
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String testProject, String projectPath, boolean copyForMutation) {

    return newContext(testProject, projectPath, copyForMutation, null);
  }

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @param copyForMutation - {@code true} to create a copy of the project that can be modified by the test, {@code false} otherwise (only to save resources
   *     if you are 100% sure that your test never modifies anything in that project.)
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} or {@code null} if not used.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String testProject, String projectPath, boolean copyForMutation, WireMockRuntimeInfo wmRuntimeInfo) {

    return newContext(testProject, projectPath, copyForMutation, wmRuntimeInfo, IdeLogLevel.TRACE);
  }

  /**
   * @param testProject the (folder)name of the project test case, in this folder a 'project' folder represents the test project in {@link #TEST_PROJECTS}.
   *     E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @param copyForMutation - {@code true} to create a copy of the project that can be modified by the test, {@code false} otherwise (only to save resources
   *     if you are 100% sure that your test never modifies anything in that project.)
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} or {@code null} if not used.
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(String testProject, String projectPath, boolean copyForMutation, WireMockRuntimeInfo wmRuntimeInfo,
      IdeLogLevel logLevel) {

    Path ideRoot = TEST_PROJECTS.resolve(testProject);
    if (copyForMutation) {
      Path ideRootCopy = TEST_PROJECTS_COPY.resolve(testProject);
      FileAccess fileAccess = new FileAccessImpl(IdeTestContextMock.get());
      fileAccess.delete(ideRootCopy);
      fileAccess.mkdirs(TEST_PROJECTS_COPY);
      fileAccess.copy(ideRoot, TEST_PROJECTS_COPY, FileCopyMode.COPY_TREE_OVERRIDE_TREE);
      ideRoot = ideRootCopy;
    }
    if (projectPath == null) {
      projectPath = "project";
    }
    Path userDir = ideRoot.resolve(projectPath);
    ToolRepositoryMock toolRepository = null;
    IdeTestContext context = new IdeTestContext(userDir, logLevel, wmRuntimeInfo);

    Path repositoryFolder = ideRoot.resolve("repository");
    if (Files.isDirectory(repositoryFolder)) {
      toolRepository = new ToolRepositoryMock(context, repositoryFolder, wmRuntimeInfo);
      context.setDefaultToolRepository(toolRepository);
    }
    return context;
  }

  /**
   * @param projectPath the relative path inside the test project where to create the context.
   * @return the {@link IdeTestContext} pointing to that project.
   */
  protected static IdeTestContext newContext(Path projectPath) {

    return new IdeTestContext(projectPath, null);
  }

  protected static IdeTestContextAssertion assertThat(IdeTestContext context) {

    return new IdeTestContextAssertion(context);
  }

  private static List<IdeProgressBarTestImpl.ProgressEvent> assertProgressEventsAndSize(AbstractIdeTestContext context, String taskName, int chunkCount,
      long maxSize) {
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar).as(taskName).isNotNull();
    List<IdeProgressBarTestImpl.ProgressEvent> eventList = progressBar.getEventList();
    assertThat(eventList).hasSize(chunkCount + 1);
    // extra case for unknown file size (indefinite progress bar)
    if (progressBar.getMaxSize() != -1L) {
      assertThat(progressBar.getMaxSize()).isEqualTo(maxSize);
    }
    return eventList;
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} with unknown file size was implemented correctly and reflects a default behaviour.
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   * @param chunkCount amount of chunks.
   */
  protected static void assertUnknownProgressBar(IdeContext context, String taskName, long maxSize, int chunkCount) {
    List<IdeProgressBarTestImpl.ProgressEvent> eventList = assertProgressEventsAndSize((AbstractIdeTestContext) context, taskName,
        chunkCount, maxSize);

    long expectedProgress = 0;
    for (IdeProgressBarTestImpl.ProgressEvent progressEvent : eventList) {
      long stepSize = progressEvent.getStepSize();
      expectedProgress += stepSize;
    }

    // Check if the cumulative progress matches the maxSize
    assertThat(expectedProgress).isEqualTo(maxSize);
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} with unknown file size was implemented correctly and reflects a default behaviour.
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   */
  protected static void assertUnknownProgressBar(IdeContext context, String taskName, long maxSize) {

    int chunkCount = (int) (maxSize / CHUNK_SIZE);
    assertUnknownProgressBar(context, taskName, maxSize, chunkCount);
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default behavior
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   * @param chunkSize size of the chunk.
   * @param chunkCount amount of chunks.
   * @param restSize remaining size.
   */
  protected static void assertProgressBar(IdeContext context, String taskName, long maxSize, long chunkSize, int chunkCount, long restSize) {

    List<IdeProgressBarTestImpl.ProgressEvent> eventList = assertProgressEventsAndSize((AbstractIdeTestContext) context, taskName,
        chunkCount, maxSize);

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
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default behaviour, chunk size is fixed.
   *
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param taskName name of the task e.g. Downloading.
   * @param maxSize initial maximum value e.g. file size.
   * @param chunkCount amount of chunks.
   * @param restSize remaining size.
   */
  protected static void assertProgressBar(IdeContext context, String taskName, long maxSize, int chunkCount, long restSize) {

    assertProgressBar(context, taskName, maxSize, CHUNK_SIZE, chunkCount, restSize);
  }

  /**
   * Checks if a {@link com.devonfw.tools.ide.io.IdeProgressBar} was implemented correctly and reflects a default behaviour, chunk size is fixed, chunk count
   * and rest size get automatically calculated.
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

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param activeIdes the names of the IDE tools (e.g. "intellij", "eclipse").
   */
  protected void verifyStartScriptsForAllWorkspacesAndAllIdes(IdeContext context, String... activeIdes) {

    verifyStartScriptsForAllWorkspaces(context, true, activeIdes);
    Set<String> inactiveIdes = new HashSet<>(IDES);
    inactiveIdes.removeAll(Set.of(activeIdes));
    verifyStartScriptsForAllWorkspaces(context, false, inactiveIdes.toArray(String[]::new));
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param ides the names of the IDE tools (e.g. "intellij", "eclipse").
   */
  protected void verifyStartScriptsForAllWorkspaces(IdeContext context, String... ides) {

    verifyStartScriptsForAllWorkspaces(context, true, ides);
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param exists - {@code true} to check if the script exists, {@code false} otherwise (check if not exists).
   * @param ides the names of the IDE tools (e.g. "intellij", "eclipse").
   */
  protected void verifyStartScriptsForAllWorkspaces(IdeContext context, boolean exists, String... ides) {

    Path workspaces = context.getIdeHome().resolve(IdeContext.FOLDER_WORKSPACES);
    for (String ide : ides) {
      for (Path workspace : context.getFileAccess().listChildren(workspaces, Files::isDirectory)) {
        verifyStartScript(context, ide, workspace.getFileName().toString(), exists);
      }
    }
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param ide the name of the IDE tool (e.g. "intellij").
   * @param workspace the name of the workspace (e.g. "main").
   * @param exists - {@code true} to check if the script exists, {@code false} otherwise (check if not exists).
   */
  protected void verifyStartScript(IdeContext context, String ide, String workspace, boolean exists) {

    String scriptExtension;
    if (context.getSystemInfo().isWindows()) {
      scriptExtension = ".bat";
    } else {
      scriptExtension = ".sh";
    }
    Path scriptPath = context.getIdeHome().resolve(ide + "-" + workspace + scriptExtension);
    if (exists) {
      assertThat(scriptPath).exists();
    } else {
      assertThat(scriptPath).doesNotExist();
    }
  }

}
