package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.tool.npm.NpmJs;
import com.devonfw.tools.ide.tool.npm.NpmJsVersion;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * {@link JsonUrlUpdater} for packages from the Npm registry.
 */
public abstract class NpmBasedUrlUpdater extends JsonUrlUpdater<NpmJs, NpmJsVersion> {

  private static final String REGISTRY_URL = "https://registry.npmjs.org/";

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + getPackageName();
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected Class<NpmJs> getJsonObjectType() {

    return NpmJs.class;
  }

  @Override
  protected Collection<NpmJsVersion> getVersionItems(NpmJs jsonObj) {

    return jsonObj.versions().getVersionMap().values();
  }

  @Override
  protected void addVersion(UrlVersion urlVersion, NpmJsVersion jsonVersionItem) {

    doAddVersion(urlVersion, jsonVersionItem.dist().tarball());
  }

  @Override
  protected String getDownloadBaseUrl() {

    return REGISTRY_URL;
  }

  protected abstract String getPackageName();
}
