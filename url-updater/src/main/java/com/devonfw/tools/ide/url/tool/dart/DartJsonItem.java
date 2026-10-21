package com.devonfw.tools.ide.url.tool.dart;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for the {@code VERSION} feed of the Dart SDK. We map only the {@code version} property we are interested in and let Jackson ignore all
 * other properties (e.g. {@code date} and {@code revision}).
 */
public record DartJsonItem(@JsonProperty("version") String version) implements JsonVersionItem {

}
