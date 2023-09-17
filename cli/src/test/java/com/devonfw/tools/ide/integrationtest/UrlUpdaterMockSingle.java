package com.devonfw.tools.ide.integrationtest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.url.updater.UrlUpdater;

/**
 * {@link JsonUrlUpdater} test mock for {@link UrlUpdater} using a single urlVersion.
 */
public class UrlUpdaterMockSingle extends UrlUpdaterMock {

  private static final Set<String> versions = new HashSet<>(Arrays.asList("1.0"));

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    doAddVersion(urlVersion, "http://localhost:8080/os/windows_x64_url.tgz", WINDOWS, X64, "123");
  }

}