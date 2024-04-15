package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.repo.DefaultToolRepository;
import com.devonfw.tools.ide.repo.ToolRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.devonfw.tools.ide.io.FileAccessImpl.defaultContentLength;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class AbstractIdeTestContext extends AbstractIdeContext {

  private final String[] answers;

  private int answerIndex;

  private final Map<String, IdeProgressBarTestImpl> progressBarMap;

  private SystemInfo systemInfo;

  /**
   * The constructor.
   *
   * @param factory the {@link Function} to create {@link IdeSubLogger} per {@link IdeLogLevel}.
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null}
   * {@link DefaultToolRepository} will be used.
   * @param answers the automatic answers simulating a user in test.
   */
  public AbstractIdeTestContext(Function<IdeLogLevel, IdeSubLogger> factory, Path userDir,
      ToolRepository toolRepository, String... answers) {

    super(IdeLogLevel.TRACE, factory, userDir, toolRepository);
    this.answers = answers;
    this.progressBarMap = new HashMap<>();
    this.systemInfo = super.getSystemInfo();
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

    if (size == 0) {
      size = defaultContentLength;
    }

    IdeProgressBarTestImpl progressBar = new IdeProgressBarTestImpl(taskName, size);
    IdeProgressBarTestImpl duplicate = this.progressBarMap.put(taskName, progressBar);
    // If we have multiple downloads, we may have an existing "Downloading" key
    if (!taskName.equals("Downloading")) {
      assert duplicate == null;
    }
    return progressBar;
  }

  @Override
  public SystemInfo getSystemInfo() {

    return this.systemInfo;
  }

  /**
   * @param systemInfo the {@link SystemInfo} to use for testing.
   * @see com.devonfw.tools.ide.os.SystemInfoMock
   */
  public void setSystemInfo(SystemInfo systemInfo) {

    this.systemInfo = systemInfo;
  }
}
