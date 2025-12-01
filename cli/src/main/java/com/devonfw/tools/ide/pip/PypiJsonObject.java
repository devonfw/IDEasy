package com.devonfw.tools.ide.pip;

import java.util.Map;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link JsonObject} for PyPI package JSON API response.
 *
 * @param releases the map of version strings to release information.
 */
public record PypiJsonObject(Map<String, Object> releases) implements JsonObject {

}
