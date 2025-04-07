package com.devonfw.tools.ide.url.tool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Test mock for {@link UrlUpdater} preparing multiple tool versions and distributions.
 */
public class UrlUpdaterMock extends AbstractUrlUpdater {

  private static final Set<String> versions = new HashSet<>(Arrays.asList("1.0", "1.1", "1.2"));

  WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public UrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getTool() {

    return "mocked";
  }

  @Override
  protected Set<String> getVersions() {
    return versions;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    doAddVersion(urlVersion, this.wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz", WINDOWS, X64, "123");
    doAddVersion(urlVersion, this.wmRuntimeInfo.getHttpBaseUrl() + "/os/linux_x64_url.tgz", LINUX, X64, "123");
    doAddVersion(urlVersion, this.wmRuntimeInfo.getHttpBaseUrl() + "/os/mac_x64_url.tgz", MAC, X64, "123");
    doAddVersion(urlVersion, this.wmRuntimeInfo.getHttpBaseUrl() + "/os/mac_Arm64_url.tgz", MAC, ARM64, "123");
  }
}
