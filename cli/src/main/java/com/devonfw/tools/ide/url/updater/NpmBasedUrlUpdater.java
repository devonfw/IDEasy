package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;

import java.util.Collection;

/**
 * {@link JsonUrlUpdater} for packages from the Npm registry.
 */
public abstract class NpmBasedUrlUpdater extends JsonUrlUpdater<NpmJsonObject> {
  private static final String REGISTRY_URL = "https://registry.npmjs.org/";

  @Override
  protected String doGetVersionUrl() {

    return REGISTRY_URL + getPackageName();
  }

  @Override
  protected Class<NpmJsonObject> getJsonObjectType() {

    return NpmJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(NpmJsonObject jsonItem, Collection<String> versions) {

    for (NpmJsonVersion item : jsonItem.getVersions().getVersions().values()) {
      String version = item.getVersion();
      addVersion(version, versions);
    }
  }

  protected String getPackageName() {

    return getTool();
  }
}
