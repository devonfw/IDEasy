package com.devonfw.tools.ide.url.updater;

import java.util.Collection;

import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;
import com.devonfw.tools.ide.tool.repository.NpmRepository;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * {@link JsonUrlUpdater} for packages from the Npm registry.
 */
public abstract class NpmBasedUrlUpdater extends JsonUrlUpdater<NpmJsonObject, NpmJsonVersion> {

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + getPackageName();
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected Class<NpmJsonObject> getJsonObjectType() {

    return NpmJsonObject.class;
  }

  @Override
  protected Collection<NpmJsonVersion> getVersionItems(NpmJsonObject jsonObj) {

    return jsonObj.versions().getVersionMap().values();
  }

  @Override
  protected void addVersion(UrlVersion urlVersion, NpmJsonVersion jsonVersionItem) {

    doAddVersion(urlVersion, jsonVersionItem.dist().tarball());
  }

  @Override
  protected String getDownloadBaseUrl() {

    return NpmRepository.REGISTRY_URL;
  }

  protected abstract String getPackageName();
}
