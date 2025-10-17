package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.git.GitContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeTestLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.repository.NpmRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepositoryMock;
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

    this(PATH_MOCK, null);
  }

  /**
   * The constructor.
   *
   * @param workingDirectory the optional {@link Path} to current working directory.
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  public IdeTestContext(Path workingDirectory, WireMockRuntimeInfo wireMockRuntimeInfo) {

    this(workingDirectory, IdeLogLevel.TRACE, wireMockRuntimeInfo);
  }

  /**
   * The constructor.
   *
   * @param workingDirectory the optional {@link Path} to current working directory.
   * @param logLevel the {@link IdeLogLevel} used as threshold for logging.
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  public IdeTestContext(Path workingDirectory, IdeLogLevel logLevel, WireMockRuntimeInfo wireMockRuntimeInfo) {

    this(new IdeTestLogger(logLevel), workingDirectory, wireMockRuntimeInfo);
  }

  private IdeTestContext(IdeTestLogger logger, Path workingDirectory, WireMockRuntimeInfo wireMockRuntimeInfo) {

    super(logger, workingDirectory, wireMockRuntimeInfo);
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

    return new IdeTestContext(Path.of("/"), null);
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
  public static String readAndResolveBaseUrl(Path file, WireMockRuntimeInfo wmRuntimeInfo) {

    try {
      String payload = Files.readString(file);
      return payload.replace(ToolRepositoryMock.VARIABLE_TESTBASEURL, wmRuntimeInfo.getHttpBaseUrl());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected NpmRepository createNpmRepository() {
    if (this.wireMockRuntimeInfo != null) {
      return new NpmRepositoryMock(this, this.wireMockRuntimeInfo);
    }
    return super.createNpmRepository();
  }
}
