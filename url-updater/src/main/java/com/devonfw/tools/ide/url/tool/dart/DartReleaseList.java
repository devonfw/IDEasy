package com.devonfw.tools.ide.url.tool.dart;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link JsonObject} for the objects list response of the {@code dart-archive} bucket (GCS JSON API, e.g.
 * {@code /storage/v1/b/dart-archive/o?prefix=...&delimiter=/}). We map only the {@code prefixes} we are interested in (the {@code /}-separated
 * "folders" below the requested prefix) and let Jackson ignore all other properties (e.g. {@code kind} and {@code nextPageToken}).
 */
public record DartReleaseList(@JsonProperty("prefixes") List<String> prefixes) implements JsonObject {

}
