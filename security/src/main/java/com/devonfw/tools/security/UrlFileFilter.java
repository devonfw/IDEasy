package com.devonfw.tools.security;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;
import com.devonfw.tools.ide.url.updater.UpdateManager;

import java.io.FileFilter;
import java.nio.file.Paths;

/**
 * This {@link FileFilter} only accepts files with name {@link UrlStatusFile#STATUS_JSON}. The paths of the accepted
 * files are then analyzed by the {@link UrlAnalyzer} to detect the available tools, editions and versions.
 */
public class UrlFileFilter implements FileFilter {

  @Override
  public boolean accept(java.io.File pathname) {

    return pathname.getName().equals(UrlStatusFile.STATUS_JSON);
  }
}
