package com.devonfw.tools.ide.url.model.file.json;

import java.util.List;

import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a CVE of a tool (inside a "security.json" file).
 */
public record CVE(String id, double severity, List<VersionRange> versions) {

}
