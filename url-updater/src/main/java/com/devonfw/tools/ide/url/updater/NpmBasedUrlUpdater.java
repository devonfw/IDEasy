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

  /**
   * The constructor.
   */
  public NpmBasedUrlUpdater() {
    super(REGISTRY_URL, REGISTRY_URL);
  }

  protected NpmBasedUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + getPackageName();
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

  protected abstract String getPackageName();
}
