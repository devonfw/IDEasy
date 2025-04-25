package com.devonfw.tools.ide.url.tool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.UrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Test mock for {@link UrlUpdater} using a single tool version and distribution.
 */
public class UrlUpdaterMockSingle extends UrlUpdaterMock {

  private static Set<String> versions = new HashSet<>(List.of("1.0"));

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public UrlUpdaterMockSingle(WireMockRuntimeInfo wmRuntimeInfo) {

    super(wmRuntimeInfo);
  }

  @Override
  protected Set<String> getVersions() {
    return versions;
  }

  /**
   * Enables the possibility to change the version which should be tested.
   *
   * @param newVersion the new Version to be set.
   */
  protected void setVersion(final String newVersion) {
    versions = Set.of(newVersion);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    doAddVersion(urlVersion, wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz", WINDOWS, X64, "123");
  }

}
