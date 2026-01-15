package com.devonfw.tools.ide.url.model.file.json;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a dependency of a tool (inside a "dependencies.json" file).
 */
public record ToolDependency(String tool, VersionRange versionRange) implements JsonObject {

}
