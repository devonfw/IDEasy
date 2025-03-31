package com.devonfw.tools.ide.url.tool.androidstudio;

import java.util.List;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for an item of Android. We map only properties that we are interested in and let jackson ignore all others.
 */
public record AndroidJsonItem(@JsonProperty("version") String version, @JsonProperty("download") List<AndroidJsonDownload> download) implements
    JsonVersionItem {

}
