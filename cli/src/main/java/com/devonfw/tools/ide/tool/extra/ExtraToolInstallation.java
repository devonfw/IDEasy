package com.devonfw.tools.ide.tool.extra;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Represents an extra installation of some tool.
 *
 * @param name the custom name of the extra installation. Should be in lower-train-case-without-special-chars syntax.
 * @param version the {@link VersionIdentifier} to install.
 * @param edition the explicit edition to use or {@code null} to use the
 *     {@link com.devonfw.tools.ide.tool.LocalToolCommandlet#getConfiguredEdition() configured edition}.
 * @see ExtraTools#getExtraInstallations(String)
 */
public record ExtraToolInstallation(String name, VersionIdentifier version, String edition) implements JsonObject {

  static final String PROPERTY_VERSION = "version";

  static final String PROPERTY_EDITION = "edition";

}
