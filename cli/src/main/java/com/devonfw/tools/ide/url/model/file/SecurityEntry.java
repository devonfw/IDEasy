package com.devonfw.tools.ide.url.model.file;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * A simple container with the information about a security entry.
 *
 * @param versionRange The version range of affected versions.
 * @param severity The severity of the security issue (0.0 - 10.0).
 * @param severityVersion The version of the severity. As of November 2023 its either v2 or v3.
 * @param cveName The CVE name.
 * @param Description The description of the security issue.
 * @param url The url to the security issue.
 */
public record SecurityEntry(VersionRange versionRange, double severity, String severityVersion, String cveName,
    String Description, String url) {
}
