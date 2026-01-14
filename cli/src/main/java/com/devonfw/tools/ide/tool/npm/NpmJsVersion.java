package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for a version of Npm. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @see NpmJs#versions()
 */
public record NpmJsVersion(String version, NpmJsDist dist) implements JsonVersionItem {

  static final String PROPERTY_VERSION = "version";

  static final String PROPERTY_DIST = "dist";

}
