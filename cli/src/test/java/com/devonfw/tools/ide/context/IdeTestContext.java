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

  private final IdeTestLogger logger;

  private GitContext gitContext;

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   */
  public IdeTestContext(Path userDir) {

    this(userDir, null);
  }

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} *
   *     {@link com.devonfw.tools.ide.repo.DefaultToolRepository} will be used.
   */
  public IdeTestContext(Path userDir, ToolRepository toolRepository) {

    this(userDir, toolRepository, IdeLogLevel.TRACE);
  }

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} *
   *     {@link com.devonfw.tools.ide.repo.DefaultToolRepository} will be used.
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   */
  public IdeTestContext(Path userDir, ToolRepository toolRepository, IdeLogLevel logLevel) {

    this(new IdeTestLogger(logLevel), userDir, toolRepository);
  }

  private IdeTestContext(IdeTestLogger logger, Path userDir, ToolRepository toolRepository) {

    super(logger, userDir, toolRepository);
    this.logger = logger;
    this.gitContext = new GitContextMock();
  }

  @Override
  public GitContext getGitContext() {
    return this.gitContext;
  }

  /**
   * @param gitContext the instance to mock {@link GitContext}.
   */
  public void setGitContext(GitContext gitContext) {

    this.gitContext = gitContext;
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

  @Override
  public String askForInput(String message) {

    return super.askForInput(message);
    // return this.inputValues.isEmpty() ? null : this.inputValues.poll();
  }

  /**
   * @return the {@link IdeTestLogger}.
   */
  public IdeTestLogger getLogger() {

    return logger;
  }
}
