package com.devonfw.tools.ide.url.tool.flutter;

import com.devonfw.tools.ide.json.JsonVersionItem;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON data object for a single release entry of a Flutter releases feed. We map only the properties we are interested in and let Jackson ignore all
 * others.
 */
public record FlutterJsonItem(@JsonProperty("version") String version, @JsonProperty("channel") String channel, @JsonProperty("archive") String archive,
    @JsonProperty("sha256") String sha256, @JsonProperty("dart_sdk_arch") String dartSdkArch)
    implements JsonVersionItem {

}
