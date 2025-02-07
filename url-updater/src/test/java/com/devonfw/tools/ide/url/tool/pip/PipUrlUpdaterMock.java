package com.devonfw.tools.ide.url.tool.pip;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Test mock for {@link PipUrlUpdater}
 */
public class PipUrlUpdaterMock extends PipUrlUpdater {

  private static final Set<String> versions = new HashSet<>(List.of("1.0"));

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public PipUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected Set<String> getVersions() {

    return versions;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, this.wmRuntimeInfo.getHttpBaseUrl() + "/pip/${version}/get-pip.py");
  }
}
