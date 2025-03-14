package com.devonfw.tools.ide.url.tool.python;

import java.util.List;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java object to represent the JSON of a Python release. Mapping just the needed Properties
 */
public record PythonRelease(@JsonProperty("version") String version, @JsonProperty("files") List<PythonFile> files) implements JsonVersionItem {

}
