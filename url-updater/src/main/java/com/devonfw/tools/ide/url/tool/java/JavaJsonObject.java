package com.devonfw.tools.ide.url.tool.java;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for Java versions from adoptium REST API.
 */
public record JavaJsonObject(List<JavaJsonVersion> versions) implements JsonObject {

}
