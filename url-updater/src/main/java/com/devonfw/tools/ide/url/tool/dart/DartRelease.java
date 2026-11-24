package com.devonfw.tools.ide.url.tool.dart;

import com.devonfw.tools.ide.json.JsonVersionItem;

/**
 * A single stable Dart release enumerated from the {@code dart-archive} bucket objects list API.
 * <p>
 * The version is the release folder name (e.g. {@code 3.13.5}).
 */
public record DartRelease(String version) implements JsonVersionItem {

}
