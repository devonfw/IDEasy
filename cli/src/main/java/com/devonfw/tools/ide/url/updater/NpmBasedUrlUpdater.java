package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;
import com.devonfw.tools.ide.tool.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * {@link JsonUrlUpdater} for packages from the Npm registry.
 */
public abstract class NpmBasedUrlUpdater extends JsonUrlUpdater<NpmJsonObject> {
  private static final String REGISTRY_URL = "https://registry.npmjs.org/";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final Logger logger = LoggerFactory.getLogger(NpmUrlUpdater.class);

  @Override
  protected String doGetVersionUrl() {

    return getBaseUrl() + getPackageName();
  }

  @Override
  protected Class<NpmJsonObject> getJsonObjectType() {

    return NpmJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(NpmJsonObject jsonItem, Collection<String> versions) {

    throw new IllegalStateException();
  }

  @Override
  protected void collectVersionsWithUrlsFromJson(NpmJsonObject jsonObj, UrlEdition edition) {

    for (NpmJsonVersion item : jsonObj.getVersions().getVersionMap().values()) {
      String version = item.getVersion();
      //TODO: this is not the right place to filter versions
      // Also missing the logging of AbstractUrlUpdater's addVersion on which versions were filtered
      if (mapVersion(version) == null) {
        continue;
      }

      if (isTimeoutExpired()) {
        break;
      }

      UrlVersion urlVersion = edition.getChild(version);
      if (urlVersion == null || isMissingOs(urlVersion)) {
        try {
          urlVersion = edition.getOrCreateChild(version);
          doAddVersion(urlVersion, item.getDist().getTarball());
          urlVersion.save();
        } catch (Exception e) {
          logger.error("For tool {} we failed to add version {}.", getToolWithEdition(), version, e);
        }

      }
    }
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    throw new IllegalStateException();
  }

  protected String getBaseUrl() {

    return REGISTRY_URL;
  }

  protected String getPackageName() {

    return getTool();
  }
}
