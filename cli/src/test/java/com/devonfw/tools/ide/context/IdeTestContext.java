package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.repo.ToolRepository;

import java.nio.file.Path;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeTestContext extends AbstractIdeTestContext {

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
   * @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} * {@link com.devonfw.tools.ide.repo.DefaultToolRepository}
   * will be used.
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
