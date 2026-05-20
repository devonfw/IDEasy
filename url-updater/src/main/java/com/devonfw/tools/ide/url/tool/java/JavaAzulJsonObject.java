package com.devonfw.tools.ide.url.tool.java;

import java.util.List;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for Java versions from Azul REST API.
 *
 * @param versions
 */
public record JavaAzulJsonObject(List<JavaAzulJsonVersion> versions) implements JsonObject {


}
