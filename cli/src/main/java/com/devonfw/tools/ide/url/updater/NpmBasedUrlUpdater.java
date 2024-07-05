package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;
import com.devonfw.tools.ide.tool.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
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
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    UrlEdition edition = tool.getOrCreateChild(getEdition());
    updateExistingVersions(edition);
    String toolWithEdition = getToolWithEdition();
    try {
      String response = doGetResponseBodyAsString(doGetVersionUrl());
      NpmJsonObject jsonObj = MAPPER.readValue(response, getJsonObjectType());

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
            logger.error("For tool {} we failed to add version {}.", toolWithEdition, version, e);
          }

        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while getting versions from JSON API " + doGetVersionUrl(), e);
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
