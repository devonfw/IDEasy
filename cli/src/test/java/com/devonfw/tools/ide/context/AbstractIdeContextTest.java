package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;

import com.devonfw.tools.ide.commandlet.CommandletManagerResetter;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;

/**
 * Abstract base class for tests that need mocked instances of {@link IdeContext}.
 */
public abstract class AbstractIdeContextTest extends Assertions {

  /** {@link #newContext(String) Name of test project} {@value}. */
  protected static final String PROJECT_BASIC = "basic";

  /** The source {@link Path} to the test projects. */
  protected static final Path PATH_PROJECTS = Paths.get("src/test/resources/ide-projects");

  // will not use eclipse-target like done in maven via eclipse profile...
  private static final Path PATH_PROJECTS_COPY = Paths.get("target/test-projects/");

  /**
   * @param projectName the (folder)name of the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @return the {@link IdeContext} pointing to that project.
   */
  protected IdeContext newContext(String projectName) {

    return newContext(projectName, null, true);
  }

  /**
   * @param projectName the (folder)name of the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @return the {@link IdeContext} pointing to that project.
   */
  protected static IdeContext newContext(String projectName, String projectPath) {

    return newContext(projectName, projectPath, true);
  }

  /**
   * @param projectName the (folder)name of the test project in {@link #PATH_PROJECTS}. E.g. "basic".
   * @param projectPath the relative path inside the test project where to create the context.
   * @param copyForMutation - {@code true} to create a copy of the project that can be modified by the test,
   *        {@code false} otherwise (only to save resources if you are 100% sure that your test never modifies anything
   *        in that project.
   * @return the {@link IdeContext} pointing to that project.
   */
  protected static IdeContext newContext(String projectName, String projectPath, boolean copyForMutation) {

    Path sourceDir = PATH_PROJECTS.resolve(projectName);
    Path userDir = sourceDir;
    if (projectPath != null) {
      userDir = sourceDir.resolve(projectPath);
    }
    CommandletManagerResetter.reset();
    IdeContext context;
    if (copyForMutation) {
      Path projectDir = PATH_PROJECTS_COPY.resolve(projectName);
      FileAccess fileAccess = new FileAccessImpl(IdeTestContextMock.get());
      fileAccess.delete(projectDir);
      fileAccess.mkdirs(PATH_PROJECTS_COPY);
      fileAccess.copy(sourceDir, projectDir, FileCopyMode.COPY_TREE_OVERRIDE_TREE);
      fileAccess.copy(PATH_PROJECTS.resolve(IdeContext.FOLDER_IDE), PATH_PROJECTS_COPY.resolve(IdeContext.FOLDER_IDE),
          FileCopyMode.COPY_TREE_OVERRIDE_TREE);
      context = new IdeTestContext(projectDir.resolve(projectPath));
    } else {
      context = new IdeTestContext(userDir);
    }
    return context;
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param level the expected {@link IdeLogLevel}.
   * @param message the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   */
  protected static void assertLogMessage(IdeContext context, IdeLogLevel level, String message) {

    assertLogMessage(context, level, message, false);
  }

  /**
   * @param context the {@link IdeContext} that was created via the {@link #newContext(String) newContext} method.
   * @param level the expected {@link IdeLogLevel}.
   * @param message the expected {@link com.devonfw.tools.ide.log.IdeSubLogger#log(String) log message}.
   * @param contains - {@code true} if the given {@code message} may only be a sub-string of the log-message to assert,
   *        {@code false} otherwise (the entire log message including potential parameters being filled in is asserted).
   */
  protected static void assertLogMessage(IdeContext context, IdeLogLevel level, String message, boolean contains) {

    IdeTestLogger logger = (IdeTestLogger) context.level(IdeLogLevel.WARNING);
    ListAssert<String> assertion = assertThat(logger.getMessages()).as(level.name() + "-Log messages");
    if (contains) {
      Condition<String> condition = new Condition<>() {
        public boolean matches(String e) {

          return e.contains(message);
        };
      };
      assertion.filteredOn(condition).isNotEmpty();
    } else {
      assertion.contains(message);
    }
  }

}
