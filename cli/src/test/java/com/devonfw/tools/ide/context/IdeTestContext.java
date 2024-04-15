package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.repo.ToolRepository;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeTestContext extends AbstractIdeTestContext {

  private Path dummyUserHome;

  private ProcessContext mockProcessContext;

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeTestContext(Path userDir, String... answers) {

    super(level -> new IdeTestLogger(level), userDir, null, answers);
  }

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} *
   * {@link com.devonfw.tools.ide.repo.DefaultToolRepository} will be used.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeTestContext(Path userDir, ToolRepository toolRepository, String... answers) {

    super(level -> new IdeTestLogger(level), userDir, toolRepository, answers);
  }

  @Override
  public IdeTestLogger level(IdeLogLevel level) {

    return (IdeTestLogger) super.level(level);
  }

  @Override
  public GitContext getGitContext() {

    return new GitContextMock();
  }

  /**
   *
   * @param dummyUserHome mock path which will be used in {@link #getUserHome()}
   */
  public void setDummyUserHome(Path dummyUserHome) {

    this.dummyUserHome = dummyUserHome;
  }

  /**
   *
   * @return a dummy UserHome path to avoid global path access in a commandlet test. The defined {@link #dummyUserHome}
   *         will be returned if it is not {@code null}, else see implementation {@link #AbstractIdeContext}.
   */
  @Override
  public Path getUserHome() {

    if (dummyUserHome != null) {
      return dummyUserHome;
    }

    return super.getUserHome();
  }

  @Override
  protected ProcessContext createProcessContext() {

    return new ProcessContextTestImpl(this);
  }

  /**
   * @return a dummy {@link IdeTestContext}.
   */
  public static IdeTestContext of() {

    return new IdeTestContext(Path.of("/"));
  }

}
