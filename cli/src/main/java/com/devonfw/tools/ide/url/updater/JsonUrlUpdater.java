package com.devonfw.tools.ide.url.updater;

import com.devonfw.tools.ide.common.JsonObject;
import com.devonfw.tools.ide.common.JsonVersionItem;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link AbstractUrlUpdater} that retrieves the {@link UrlVersion versions} of a {@link UrlEdition tool edition} from a
 * HTTP response with JSON body.
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
    UrlEdition edition = tool.getOrCreateChild(getEdition());
    updateExistingVersions(edition);
    try {
      String response = doGetResponseBodyAsString(doGetVersionUrl());
      J jsonObj = MAPPER.readValue(response, getJsonObjectType());
      collectVersionsWithDownloadsFromJson(jsonObj, edition);
    } catch (Exception e) {
      throw new IllegalStateException("Error while getting versions from JSON API " + doGetVersionUrl(), e);
    }
  }

  @Override
  protected Set<String> getVersions() {

    String url = doGetVersionUrl();
    Set<String> versions = new HashSet<>();
    try {
      String response = doGetResponseBodyAsString(url);
      J jsonObject = MAPPER.readValue(response, getJsonObjectType());
      collectVersionsFromJson(jsonObject, versions);
    } catch (Exception e) {
      throw new IllegalStateException("Error while getting versions from JSON API " + url, e);
    }
    return versions;
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
   * @param jsonObject the Java object parsed from the JSON holding the information of the versions.
   * @param versions the versions where to {@link #addVersion(String, Collection) add the version to}.
   */
  @Deprecated
  protected abstract void collectVersionsFromJson(J jsonObject, Collection<String> versions);

  protected void collectVersionsWithDownloadsFromJson(J jsonObj, UrlEdition edition) {

    Set<String> versions = new HashSet<>();

    for (JVI item : getVersionItems(jsonObj)) {
      String version = getVersion(item);
      if (!addVersion(version, versions))
        continue;

      if (isTimeoutExpired()) {
        break;
      }

      UrlVersion urlVersion = edition.getChild(version);
      if (urlVersion == null || isMissingOs(urlVersion)) {
        try {
          urlVersion = edition.getOrCreateChild(version);
          doAddVersion(urlVersion, getDownloadUrl(item));
          urlVersion.save();
        } catch (Exception e) {
          logger.error("For tool {} we failed to add version {}.", getToolWithEdition(), version, e);
        }

      }
    }
  }

  //mapping from whole JSON to the individual version entries
  protected abstract Collection<JVI> getVersionItems(J jsonObject);

  //getting the download url from one item
  protected abstract String getDownloadUrl(JVI jsonVersionItem);

  //getting the version from one item
  protected abstract String getVersion(JVI jsonVersionItem);

}
