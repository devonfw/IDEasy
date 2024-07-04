package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.npm.NpmJsonVersion;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.NpmBasedUrlUpdater;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NpmBasedUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends NpmBasedUrlUpdater {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final Logger logger = LoggerFactory.getLogger(NpmUrlUpdater.class);

  @Override
  protected String getTool() {

    return "npm";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

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

      for (NpmJsonVersion item : jsonObj.getVersions().getVersions().values()) {
        String version = item.getVersion();

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

}