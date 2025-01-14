package com.devonfw.tools.ide.repo;

import java.util.List;

/**
 * {@link CustomToolsJson} for the ide-custom-tools.json file.
 *
 * @param url the repository base URL. This may be a typical URL (e.g. "https://host/path") but may also be a path in your file-system (e.g. to a mounted
 *     remote network drive).
 * @param tools the {@link List} of {@link CustomToolJson}.
 */
public record CustomToolsJson(String url, List<CustomToolJson> tools) {

}
