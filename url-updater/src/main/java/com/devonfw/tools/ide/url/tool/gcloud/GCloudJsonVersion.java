package com.devonfw.tools.ide.url.tool.gcloud;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * JSON data object for the Google Cloud CLI version. Only the root version is mapped; component metadata is ignored.
 */
public record GCloudJsonVersion(String version) implements JsonVersionItem {

}
