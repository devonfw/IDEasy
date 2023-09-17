package com.devonfw.tools.ide.url.updater;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.devonfw.tools.ide.common.JsonObject;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AbstractUrlUpdater} that retrieves the {@link UrlVersion versions} of a {@link UrlEdition tool edition} from a
 * HTTP response with JSON body.
 *
 * @param <J> type of the {@link JsonObject}.
 */
public abstract class JsonUrlUpdater<J extends JsonObject> extends AbstractUrlUpdater {

  private static final ObjectMapper MAPPER = JsonMapping.create();

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
   * @param jsonItem the Java object parsed from the JSON holding the information of a version.
   * @param versions the versions where to {@link #addVersion(String, Collection) add the version to}.
   */
  protected abstract void collectVersionsFromJson(J jsonItem, Collection<String> versions);
}
