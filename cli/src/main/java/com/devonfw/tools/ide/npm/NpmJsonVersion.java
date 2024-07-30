package com.devonfw.tools.ide.npm;

import com.devonfw.tools.ide.common.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a version of Npm. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @see NpmJsonObject#versions()
 */
public record NpmJsonVersion(@JsonProperty("version") String version, @JsonProperty("dist") NpmJsonDist dist) implements JsonVersionItem {

}
