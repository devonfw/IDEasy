package com.devonfw.tools.ide.npm;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Npm.
 */
public record NpmJsonObject(NpmJsonVersions versions) implements JsonObject {

}
