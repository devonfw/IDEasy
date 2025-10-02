package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock class for {@link MvnRepository}.
 */
public class MvnRepositoryMock extends MvnRepository {

  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  public MvnRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {
    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
  }
}
