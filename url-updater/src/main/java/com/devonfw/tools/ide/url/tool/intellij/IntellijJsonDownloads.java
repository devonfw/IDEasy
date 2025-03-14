package com.devonfw.tools.ide.url.tool.intellij;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for an item of Intellij. We map only properties that we are interested in and let jackson ignore all others.
 */
public record IntellijJsonDownloads(@JsonProperty("downloads") List<IntellijJsonDownloadsItem> downloads) {

}
