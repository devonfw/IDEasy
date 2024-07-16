package com.devonfw.tools.ide.tool.pip;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Test mock for {@link PipUrlUpdater}
 */
public class PipUrlUpdaterMock extends PipUrlUpdater {

  private final static String TEST_BASE_URL = "http://localhost:8080";

  private static final Set<String> versions = new HashSet<>(List.of("1.0"));

  @Override
  protected Set<String> getVersions() {

    return versions;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, TEST_BASE_URL + "/pip/${version}/get-pip.py");
  }
}
