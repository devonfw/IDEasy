package com.devonfw.tools.ide.npm;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for {@link com.devonfw.tools.ide.tool.npm.Npm npm}.
 *
 * @param versions the {@link NpmJsonVersions} with all the version history of the artifact.
 */
public record NpmJsonObject(NpmJsonVersions versions) implements JsonObject {

}
