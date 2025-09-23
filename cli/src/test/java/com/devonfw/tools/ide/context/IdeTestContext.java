package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Implementation of {@link IdeContext} for testing.
 */
public class IdeTestContext extends AbstractIdeTestContext {

  private final IdeTestLogger logger;

  private GitContext gitContext;

  /**
   * The constructor.
   */
  public IdeTestContext() {

    this(PATH_MOCK);
  }

  /**
   * The constructor.
   *
   * @param workingDirectory the optional {@link Path} to current working directory.
   */
  public IdeTestContext(Path workingDirectory) {

    this(workingDirectory, IdeLogLevel.TRACE);
  }

  /**
   * The constructor.
   *
   * @param workingDirectory the optional {@link Path} to current working directory.
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   */
  public IdeTestContext(Path workingDirectory, IdeLogLevel logLevel) {

    this(new IdeTestLogger(logLevel), workingDirectory);
  }

  private IdeTestContext(IdeTestLogger logger, Path workingDirectory) {

    super(logger, workingDirectory);
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

  /**
   * @return the {@link IdeTestLogger}.
   */
  public IdeTestLogger getLogger() {

    return logger;
  }

  /**
   * Reads the content of the given file and replaces the placeholder "${testbaseurl}" with the actual base URL. Copy from AbstractUrlUpdaterTest.
   *
   * @param file the {@link Path} to the file to read.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} providing the base URL.
   * @return the resolved file content.
   */
  public static String readAndResolve(Path file, WireMockRuntimeInfo wmRuntimeInfo) {

    try {
      String payload = Files.readString(file);
      return payload.replace("${testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
