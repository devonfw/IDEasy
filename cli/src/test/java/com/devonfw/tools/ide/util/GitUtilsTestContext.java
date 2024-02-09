package com.devonfw.tools.ide.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class GitUtilsTestContext extends AbstractIdeTestContext {

  private List<String> errors;

  private List<String> outs;

  private int exitCode;

  private Path directory;

  private static boolean testOnlineMode;

  /**
   * The constructor.
   *
   * @param isOnline boolean if it should be run in online mode.
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public GitUtilsTestContext(boolean isOnline, Path userDir, String... answers) {

    super(level -> new IdeTestLogger(level), userDir, answers);
    testOnlineMode = isOnline;
    this.errors = new ArrayList<>();
    this.outs = new ArrayList<>();
    this.exitCode = 0;
    this.directory = userDir;
  }

  @Override
  public boolean isOnline() {

    return testOnlineMode;
  }

  @Override
  public IdeTestLogger level(IdeLogLevel level) {

    return (IdeTestLogger) super.level(level);
  }

  /**
   * @return a dummy {@link GitUtilsTestContext}.
   */
  public static GitUtilsTestContext of() {

    return new GitUtilsTestContext(testOnlineMode, Paths.get("/"));
  }

  @Override
  public ProcessContext newProcess() {

    return new GitUtilsProcessContextMock(this.errors, this.outs, this.exitCode, this.directory);
  }

  public void setErrors(List<String> errors) {

    this.errors = errors;
  }

  public void setOuts(List<String> outs) {

    this.outs = outs;
  }

  public void setExitCode(int exitCode) {

    this.exitCode = exitCode;
  }

  public void setDirectory(Path directory) {

    this.directory = directory;
  }

}
