package com.devonfw.tools.ide.context;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class AbstractIdeTestContext extends AbstractIdeContext {

  private final String[] answers;

  private int answerIndex;

  private final Map<String, IdeProgressBarTestImpl> progressBarMap;

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
    this.progressBarMap = new HashMap<>();
  }

  @Override
  public boolean isTest() {

    return true;
  }

  @Override
  protected String readLine() {

    if (this.answerIndex >= this.answers.length) {
      throw new IllegalStateException("End of answers reached!");
    }
    return this.answers[this.answerIndex++];
  }

  /**
   * @return Map of progress bars with task name and actual implementation.
   */
  public Map<String, IdeProgressBarTestImpl> getProgressBarMap() {

    return this.progressBarMap;
  }

  @Override
  public IdeProgressBar prepareProgressBar(String taskName, long size) {

    IdeProgressBarTestImpl progressBar = new IdeProgressBarTestImpl(taskName, size);
    IdeProgressBarTestImpl duplicate = this.progressBarMap.put(taskName, progressBar);
    // If we have multiple downloads, we may have an existing "Downloading" key
    if (!taskName.equals("Downloading")) {
      assert duplicate == null;
    }
    return progressBar;
  }

}
