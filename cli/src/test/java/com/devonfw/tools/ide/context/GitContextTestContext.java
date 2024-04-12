package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;

/**
 * Implementation of {@link IdeContext} for testing {@link GitContext}.
 */
public class GitContextTestContext extends AbstractIdeTestContext {

  private List<String> errors;

  private List<String> outs;

  private int exitCode;

  private static boolean testOnlineMode;

  /**
   * The constructor.
   *
   * @param isOnline boolean if it should be run in online mode.
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public GitContextTestContext(boolean isOnline, Path userDir, String... answers) {

    super(level -> new IdeTestLogger(level), userDir, null, answers);
    testOnlineMode = isOnline;
    this.errors = new ArrayList<>();
    this.outs = new ArrayList<>();
    this.exitCode = 0;
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
   * @return a dummy {@link GitContextTestContext}.
   */
  public static GitContextTestContext of() {

    return new GitContextTestContext(testOnlineMode, Paths.get("/"));
  }

  @Override
  public ProcessContext newProcess() {

    return new GitContextProcessContextMock(this.errors, this.outs, this.exitCode, getCwd());
  }

  /**
   * @param errors the {@link List} of errors (stderr lines) of the mocked git process.
   */
  public void setErrors(List<String> errors) {

    this.errors = errors;
  }

  /**
   * @param outs the {@link List} of outputs (stdout lines) of the mocked git process.
   */
  public void setOuts(List<String> outs) {

    this.outs = outs;
  }

  /**
   * @param exitCode the return code of the mocked git process.
   */
  public void setExitCode(int exitCode) {

    this.exitCode = exitCode;
  }

}
