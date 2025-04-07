package com.devonfw.tools.ide.url.tool.androidstudio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a content of Android. We map only properties that we are interested in and let jackson ignore all others.
 */
public record AndroidJsonContent(@JsonProperty("item") List<AndroidJsonItem> item) {

}
