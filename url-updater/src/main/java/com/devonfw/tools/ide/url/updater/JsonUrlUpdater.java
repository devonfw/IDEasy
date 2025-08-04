package com.devonfw.tools.ide.url.updater;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.json.JsonVersionItem;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractUrlUpdater} that retrieves the {@link UrlVersion versions} of a {@link UrlEdition tool edition} from an HTTP response with JSON body.
 *
 * @param <J> type of the {@link JsonObject}.
 */
public abstract class JsonUrlUpdater<J extends JsonObject, JVI extends JsonVersionItem> extends AbstractUrlUpdater {

  private static final ObjectMapper MAPPER = JsonMapping.create();
  private static final Logger logger = LoggerFactory.getLogger(JsonUrlUpdater.class);

  /**
   * Updates the tool's versions in the URL repository.
   *
   * @param urlRepository the {@link UrlRepository} to update
   */
  @Override
  public void update(UrlRepository urlRepository) {

    UrlTool tool = urlRepository.getOrCreateChild(getTool());
    try {
      String response = doGetResponseBodyAsString(doGetVersionUrl());
      for (String edition : getEditions()) {
        J jsonObj = getJsonObjectFromResponse(response, edition);
        if (jsonObj != null) {
          UrlEdition urlEdition = tool.getOrCreateChild(edition);
          setUrlUpdaterReport(new UrlUpdaterReport(tool.getName(), urlEdition.getName()));
          updateExistingVersions(urlEdition);
          collectVersionsWithDownloadsFromJson(jsonObj, urlEdition);
        }
      }
      getUrlFinalReport().addUrlUpdaterReport(getUrlUpdaterReport());
    } catch (Exception e) {
      throw new IllegalStateException("Error while getting versions from JSON API " + doGetVersionUrl(), e);
    }
  }

  @Override
  protected Set<String> getVersions() {

    throw new IllegalStateException();

  }

  /**
   * @return the URL of the JSON API to get the versions from.
   */
  protected abstract String doGetVersionUrl();

  /**
   * @return the {@link Class} reflecting the Java object the JSON shall be mapped to.
   */
  protected abstract Class<J> getJsonObjectType();

  /**
   * @param jsonObj the Java object parsed from the JSON holding the information of the versions.
   * @param edition the edition where to {@link #addVersion(UrlVersion, JsonVersionItem)} add the version to.
   */
  protected void collectVersionsWithDownloadsFromJson(J jsonObj, UrlEdition edition) {

    Set<String> versions = new HashSet<>();

    for (JVI item : getVersionItems(jsonObj)) {
      String version = getVersion(item);
      if (!addVersion(version, versions)) {
        continue;
      }
      version = mapVersion(version);

      if (isTimeoutExpired()) {
        break;
      }

      UrlVersion urlVersion = edition.getChild(version);
      if (urlVersion == null || isMissingOs(urlVersion)) {
        try {
          urlVersion = edition.getOrCreateChild(version);
          addVersion(urlVersion, item);
          urlVersion.save();
        } catch (Exception e) {
          logger.error("For tool {} we failed to add version {}.", getToolWithEdition(edition.getName()), version, e);
        }

      }
    }
  }

  /**
   * Gets the {@link JsonObject} from the response of the version URL.
   *
   * @param response String from the JSON API request
   * @param edition the data to get from the response corresponding to a specific edition.
   * @return {@link JsonObject} holding the available versions and possibly download urls of the tool.
   */
  protected J getJsonObjectFromResponse(String response, String edition) throws JsonProcessingException {
    return MAPPER.readValue(response, getJsonObjectType());
  }

  /**
   * Splits the json object that contains information about all available versions into a collection of individual version items.
   *
   * @param jsonObject the Java object parsed from the JSON holding the information of the versions
   * @return Collection of individual version items
   */
  protected abstract Collection<JVI> getVersionItems(J jsonObject);

  /**
   * Get the tool version of one {@link JVI}
   *
   * @param jsonVersionItem the json item containing information of one version of the tool
   * @return the version of the item
   */
  protected String getVersion(JVI jsonVersionItem) {
    return jsonVersionItem.version();
  }

  /**
   * Updates the version of a given URL version. Replaces {@link AbstractUrlUpdater#addVersion(UrlVersion)}, when download links are available from within the
   * json
   *
   * @param urlVersion the {@link UrlVersion} to be updated
   * @param jsonVersionItem the {@link JsonVersionItem} holding the information about a specific version and possibly download links
   */
  protected void addVersion(UrlVersion urlVersion, JVI jsonVersionItem) {

    addVersion(urlVersion);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    throw new UnsupportedOperationException();
  }

}
