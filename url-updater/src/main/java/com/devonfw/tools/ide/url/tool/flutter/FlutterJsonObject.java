package com.devonfw.tools.ide.url.tool.flutter;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link JsonObject} for a Flutter releases feed (one feed per operating system, e.g. {@code releases_linux.json}). We map only the properties we are
 * interested in and let Jackson ignore all others.
 */
public record FlutterJsonObject(@JsonProperty("base_url") String baseUrl, @JsonProperty("releases") List<FlutterJsonItem> releases) implements JsonObject {

}
