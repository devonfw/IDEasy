package com.devonfw.tools.security;

import java.io.FileFilter;

import static com.devonfw.tools.ide.url.model.file.UrlStatusFile.STATUS_JSON;

public class UrlFileFilter implements FileFilter {

  public UrlFileFilter() {

  }

  @Override
  public boolean accept(java.io.File pathname) {

//    System.out.println("UrlFileFilter.accept()" +  pathname.getName().equals(STATUS_JSON));
//    return pathname.getName().endsWith("urls") && pathname.getName().startsWith("windows");
    return pathname.getName().equals(STATUS_JSON);
  }
}
