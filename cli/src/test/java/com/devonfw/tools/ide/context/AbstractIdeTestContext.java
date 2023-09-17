package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.util.function.Function;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class AbstractIdeTestContext extends AbstractIdeContext {

  private final String[] answers;

  private int answerIndex;

  /**
   * The constructor.
   *
   * @param factory the {@link Function} to create {@link IdeSubLogger} per {@link IdeLogLevel}.
   * @param userDir the optional {@link Path} to current working directory.
   * @param answers the automatic answers simulating a user in test.
   */
  public AbstractIdeTestContext(Function<IdeLogLevel, IdeSubLogger> factory, Path userDir, String... answers) {

    super(IdeLogLevel.TRACE, factory, userDir);
    this.answers = answers;
  }

  @Override
  protected boolean isTest() {

    return true;
  }

  @Override
  protected String readLine() {

    if (this.answerIndex >= this.answers.length) {
      throw new IllegalStateException("End of answers reached!");
    }
    return this.answers[this.answerIndex++];
  }

}
