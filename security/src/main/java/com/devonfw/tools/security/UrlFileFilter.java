package com.devonfw.tools.security;

import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.devonfw.tools.ide.url.model.file.UrlStatusFile;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.UpdateManager;

/**
 * This {@link FileFilter} only accepts files with name {@link UrlStatusFile#STATUS_JSON}. The paths of the accepted
 * files are then analyzed by the {@link UrlAnalyzer} to detect the available tools, editions and versions.
 */
public class UrlFileFilter implements FileFilter {

  UpdateManager updateManager = new UpdateManager(Paths.get("C:\\projects\\_ide\\urls"), null);


  @Override
  public boolean accept(java.io.File pathname) {

    boolean found = pathname.getName().equals(UrlStatusFile.STATUS_JSON);
    if (found) {
      Path versionFolder = Paths.get(pathname.getPath()).getParent();
      String tool = versionFolder.getParent().getParent().getFileName().toString();
      String edition = versionFolder.getParent().getFileName().toString();
      AbstractUrlUpdater urlUpdater = updateManager.retrieveUrlUpdater(tool, edition);
      System.out.println("UrlAnalyzer: tool = " + tool + ", edition = " + edition + ", urlUpdater = " + urlUpdater);
    }

    return pathname.getName().equals(UrlStatusFile.STATUS_JSON);
  }
}
