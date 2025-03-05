package com.devonfw.tools.ide.url.tool.androidstudio;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.tool.androidstudio.AndroidJsonContent;

/**
 * {@link JsonObject} for Android Studio content.
 */
public record AndroidJsonObject(AndroidJsonContent content) implements JsonObject {

}
