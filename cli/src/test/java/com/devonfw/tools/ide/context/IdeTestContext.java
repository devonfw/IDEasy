package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLoggerFactory;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.repo.ToolRepository;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeTestContext extends AbstractIdeTestContext {

  private final IdeTestLoggerFactory loggerFactory;

  private LinkedList<String> inputValues;

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeTestContext(Path userDir, String... answers) {

    this(userDir, null, answers);
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

    this(userDir, toolRepository, IdeLogLevel.TRACE, answers);
  }

  /**
   * The constructor.
   *
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} * {@link com.devonfw.tools.ide.repo.DefaultToolRepository}
   * will be used.
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   * @param answers the automatic answers simulating a user in test.
   */
  public IdeTestContext(Path userDir, ToolRepository toolRepository, IdeLogLevel logLevel, String... answers) {

    this(new IdeTestLoggerFactory(logLevel), userDir, toolRepository, answers);
  }

  private IdeTestContext(IdeTestLoggerFactory loggerFactory, Path userDir, ToolRepository toolRepository, String... answers) {

    super(loggerFactory, userDir, toolRepository, answers);
    this.loggerFactory = loggerFactory;
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

  /**
   * Set a mocked value to be returned by the {@link IdeContext#askForInput(String)} method
   *
   * @param values a {@link LinkedList} with the mocked input value
   */
  public void setInputValues(List<String> values) {

    this.inputValues = new LinkedList<>(values);
  }

  @Override
  public String askForInput(String message) {

    return this.inputValues.isEmpty() ? null : this.inputValues.poll();
  }

  /**
   * @return the {@link IdeTestLoggerFactory}.
   */
  public IdeTestLoggerFactory getLoggerFactory() {

    return loggerFactory;
  }
}
