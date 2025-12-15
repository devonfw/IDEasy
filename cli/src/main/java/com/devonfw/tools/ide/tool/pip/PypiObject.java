package com.devonfw.tools.ide.tool.pip;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link JsonObject} for PyPI package JSON API response.
 *
 * @param releases the map of version strings to release information.
 */
public record PypiObject(List<VersionIdentifier> releases) implements JsonObject {

  static final String PROPERTY_RELEASES = "releases";

}
