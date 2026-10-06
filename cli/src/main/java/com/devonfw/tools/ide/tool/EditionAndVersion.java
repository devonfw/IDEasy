package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Simple record holding an installed tool's edition and version together.
 */
public record EditionAndVersion(String edition, VersionIdentifier version) {

}
