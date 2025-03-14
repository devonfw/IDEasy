package com.devonfw.tools.ide.url.tool.androidstudio;

import com.devonfw.tools.ide.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a download of Android. We map only properties that we are interested in and let jackson ignore all others.
 */
public record AndroidJsonDownload(@JsonProperty("link") String link, @JsonProperty("checksum") String checksum) implements JsonObject {

}
