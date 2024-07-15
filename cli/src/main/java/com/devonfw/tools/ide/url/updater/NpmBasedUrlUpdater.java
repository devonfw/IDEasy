package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

import java.util.Collection;

/**
 * {@link JsonUrlUpdater} for packages from the Npm registry.
 */
public abstract class NpmBasedUrlUpdater extends JsonUrlUpdater<NpmJsonObject, NpmJsonVersion> {
  private static final String REGISTRY_URL = "https://registry.npmjs.org/";

  @Override
  protected String doGetVersionUrl() {

    return getBaseUrl() + getPackageName();
  }

  @Override
  protected Class<NpmJsonObject> getJsonObjectType() {

    return NpmJsonObject.class;
  }

  @Override
  protected Collection<NpmJsonVersion> getVersionItems(NpmJsonObject jsonObj) {

    return jsonObj.getVersions().getVersionMap().values();
  }

  @Override
  protected String getVersion(NpmJsonVersion jsonObj) {

    return jsonObj.getVersion();
  }

  @Override
  protected void addVersion(UrlVersion urlVersion, NpmJsonVersion jsonVersionItem) {

    doAddVersion(urlVersion, jsonVersionItem.getDist().getTarball());
  }

  protected String getBaseUrl() {

    return REGISTRY_URL;
  }

  //TODO make abstract
  protected String getPackageName() {

    return getTool();
  }
}
