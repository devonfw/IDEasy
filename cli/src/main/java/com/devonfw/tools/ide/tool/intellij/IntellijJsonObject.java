package com.devonfw.tools.ide.tool.intellij;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for Intellij content.
 */
public record IntellijJsonObject(List<IntellijJsonRelease> releases) implements JsonObject {

}
