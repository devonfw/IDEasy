package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.tool.repository.NpmRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

public class NpmRepositoryMock extends NpmRepository {

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public NpmRepositoryMock(IdeContext context, WireMockRuntimeInfo wmRuntimeInfo) {

    super(context);
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  public String getRegistryUrl() {

    return wmRuntimeInfo.getHttpBaseUrl() + "/";
  }
}
