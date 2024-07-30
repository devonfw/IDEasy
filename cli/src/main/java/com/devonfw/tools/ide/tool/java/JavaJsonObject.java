package com.devonfw.tools.ide.tool.java;

import java.util.List;

import com.devonfw.tools.ide.common.JsonObject;

/**
 * {@link JsonObject} for Java versions from adoptium REST API.
 */
public record JavaJsonObject(List<JavaJsonVersion> versions) implements JsonObject {

}
