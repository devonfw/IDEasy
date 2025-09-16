package com.devonfw.tools.ide.npm;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for {@link com.devonfw.tools.ide.tool.npm.Npm npm}.
 */
public record NpmJsonObject(NpmJsonVersions versions) implements JsonObject {

}
