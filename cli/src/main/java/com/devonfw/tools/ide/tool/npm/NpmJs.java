package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for {@link com.devonfw.tools.ide.tool.npm.Npm npm} from <a href="https://registry.npmjs.org/">npmjs</a>.
 *
 * @param versions the {@link NpmJsVersions} with all the version history of the artifact.
 */
public record NpmJs(NpmJsVersions versions) implements JsonObject {

  static final String PROPERTY_VERSIONS = "versions";

}
