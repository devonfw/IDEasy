package com.devonfw.tools.ide.url.tool.intellij;

import java.util.List;

import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.url.updater.IdeaBasedUrlUpdater;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link IdeaBasedUrlUpdater} base class for IntelliJ.
 */
public class IntellijUrlUpdater extends IdeaBasedUrlUpdater {

  private static final String JSON_URL = "products?code=IIU%2CIIC&release.type=release";
  protected static final List<String> EDITIONS = List.of("ultimate", "intellij");
  protected static final ObjectMapper MAPPER = JsonMapping.create();

  @Override
  protected String getTool() {

    return "intellij";
  }

  @Override
  protected List<String> getEditions() {
    return EDITIONS;
  }

  @Override
  protected String doGetVersionUrl() {

    return getVersionBaseUrl() + "/" + JSON_URL;
  }

  @Override
  protected IntellijJsonObject getJsonObjectFromResponse(String response, String edition) throws JsonProcessingException {
    IntellijJsonObject[] jsonObjects = MAPPER.readValue(response, IntellijJsonObject[].class);
    return jsonObjects[EDITIONS.indexOf(edition)];
  }


  @Override
  public String getCpeVendor() {
    return "jetbrains";
  }

  @Override
  public String getCpeProduct() {
    return "intellij";
  }


}
