package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.common.SystemPath;
import com.devonfw.tools.ide.environment.AbstractEnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesPropertiesFile;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.IdeProgressBar;
import com.devonfw.tools.ide.io.IdeProgressBarTestImpl;
import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.repo.DefaultToolRepository;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class AbstractIdeTestContext extends AbstractIdeContext {

  private String[] answers;

  private int answerIndex;

  private final Map<String, IdeProgressBarTestImpl> progressBarMap;

  private SystemInfo systemInfo;

  private Path dummyUserHome;

  private Boolean online;

  private ProcessContext mockContext;

  /**
   * The constructor.
   *
   * @param logger the {@link IdeLogger}.
   * @param userDir the optional {@link Path} to current working directory.
   * @param toolRepository @param toolRepository the {@link ToolRepository} of the context. If it is set to {@code null} {@link DefaultToolRepository} will
   *     be used.
   * @param answers the automatic answers simulating a user in test.
   */
  public AbstractIdeTestContext(IdeStartContextImpl logger, Path userDir, ToolRepository toolRepository, String... answers) {

    super(logger, userDir, toolRepository);
    this.answers = new String[0];
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

  public void setAnswers(String... answers) {
    this.answers = answers;
    this.answerIndex = 0;
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
    // If we have multiple downloads or unpacking, we may have an existing "Downloading" or "Unpacking" key
    if ((!taskName.equals("Downloading")) && (!taskName.equals("Unpacking"))) {
      assert duplicate == null;
    }
    return progressBar;
  }

  @Override
  protected AbstractEnvironmentVariables createSystemVariables() {

    Path home = getUserHome();
    if (home != null) {
      Path systemPropertiesFile = home.resolve("environment.properties");
      if (Files.exists(systemPropertiesFile)) {
        return new EnvironmentVariablesPropertiesFile(null, EnvironmentVariablesType.SYSTEM, systemPropertiesFile, this);
      }
    }
    return super.createSystemVariables();
  }

  @Override
  protected SystemPath computeSystemPath() {

    EnvironmentVariables systemVars = getVariables().getByType(EnvironmentVariablesType.SYSTEM);
    String envPath = systemVars.get(IdeVariables.PATH.getName());
    envPath = getVariables().resolve(envPath, systemVars.getSource());
    return new SystemPath(this, envPath);
  }

  @Override
  public boolean isOnline() {

    if (this.online != null) {
      return this.online.booleanValue();
    }
    return super.isOnline();
  }

  /**
   * @param online the mocked {@link #isOnline()} result.
   */
  public void setOnline(Boolean online) {

    this.online = online;
  }

  @Override
  public ProcessContext newProcess() {

    if (this.mockContext != null) {
      return this.mockContext;
    }
    return super.newProcess();
  }

  /**
   * @param mockContext the instance to mock {@link #newProcess()}.
   */
  public void setProcessContext(ProcessContext mockContext) {

    this.mockContext = mockContext;
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

  /**
   * @param dummyUserHome mock path which will be used in {@link #getUserHome()}
   */
  public void setUserHome(Path dummyUserHome) {

    this.dummyUserHome = dummyUserHome;
  }

  /**
   * @return a dummy UserHome path to avoid global path access in a commandlet test. The defined {@link #dummyUserHome} will be returned if it is not
   *     {@code null}, else see implementation {@link #AbstractIdeContext}.
   */
  @Override
  public Path getUserHome() {

    if (this.dummyUserHome != null) {
      return this.dummyUserHome;
    }
    return super.getUserHome();
  }
}
