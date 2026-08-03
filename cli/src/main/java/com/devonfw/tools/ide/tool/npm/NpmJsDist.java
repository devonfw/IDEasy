package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * JSON data object for a version of Npm. We map only properties that we are interested in and let jackson ignore all others.
 *
 * @see NpmJs#versions()
 */
public record NpmJsDist(String tarball) implements JsonObject {

  static final String PROPERTY_TARBALL = "tarball";

}
