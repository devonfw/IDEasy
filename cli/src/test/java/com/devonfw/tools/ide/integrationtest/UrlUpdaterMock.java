package com.devonfw.tools.ide.integrationtest;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.url.updater.UrlUpdater;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test mock for {@link UrlUpdater} preparing multiple tool versions and distributions.
 */
public class UrlUpdaterMock extends AbstractUrlUpdater {

  private static final Set<String> versions = new HashSet<>(Arrays.asList("1.0", "1.1", "1.2"));

  @Override
  protected String getTool() {

    return "mocked";
  }

  @Override
  public void update(UrlRepository urlRepository) {
    super.update(urlRepository);
  }

  @Override
  protected Set<String> getVersions() {
    return versions;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    doAddVersion(urlVersion, "http://localhost:8080/os/windows_x64_url.tgz", WINDOWS, X64, "123");
    doAddVersion(urlVersion, "http://localhost:8080/os/linux_x64_url.tgz", LINUX, X64, "123");
    doAddVersion(urlVersion, "http://localhost:8080/os/mac_x64_url.tgz", MAC, X64, "123");
    doAddVersion(urlVersion, "http://localhost:8080/os/mac_Arm64_url.tgz", MAC, ARM64, "123");
  }

}
