package com.devonfw.tools.IDEasy.dev;

import java.io.FileFilter;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;

/**
 * This {@link FileFilter} only accepts files with name {@link UrlStatusFile#STATUS_JSON}. The paths of the accepted files are then analyzed by the
 * {@link UrlAnalyzer} to detect the available tools, editions and versions.
 */
public class UrlFileFilter implements FileFilter {

  /**
   * This method only accepts files with name {@link UrlStatusFile#STATUS_JSON}.
   *
   * @param pathname the {@link java.io.File} to check.
   */
  @Override
  public boolean accept(java.io.File pathname) {

    return pathname.getName().equals(UrlStatusFile.STATUS_JSON);
  }
}
