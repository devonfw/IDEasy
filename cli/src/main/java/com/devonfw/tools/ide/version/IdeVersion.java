package com.devonfw.tools.ide.version;

import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * Class to {@link #getVersionString()} the current version of this IDE product.
 */
public final class IdeVersion {

  /** The fallback version used if the version is undefined (in local development). */
  public static final String VERSION_UNDEFINED = "SNAPSHOT";

  private static final IdeVersion INSTANCE = new IdeVersion();

  private final String version;

  private final VersionIdentifier versionIdentifier;

  // most simple solution would be maven filtering but that is kind of tricky for java files
  // http://www.mojohaus.org/templating-maven-plugin/examples/source-filtering.html
  // private static final String VERSION = "${project.version}";

  private IdeVersion() {

    super();
    String v = getClass().getPackage().getImplementationVersion();
    if (v == null) {
      v = VERSION_UNDEFINED;
    }
    this.version = v;
    this.versionIdentifier = VersionIdentifier.of(v);
  }

  private String getValue(Manifest manifest, Name name) {

    return manifest.getMainAttributes().getValue(name);
  }

  /**
   * @return the current version of this IDE product as {@link String}.
   */
  public static String getVersionString() {

    return INSTANCE.version;
  }

  /**
   * @return the current version of this IDE product as {@link VersionIdentifier}.
   */
  public static VersionIdentifier getVersionIdentifier() {

    return INSTANCE.versionIdentifier;
  }

  /**
   * @return {@code true} if the {@link #getVersionString() current version} is {@link #VERSION_UNDEFINED undefined}, {@code false} otherwise.
   */
  public static boolean isUndefined() {

    return VERSION_UNDEFINED.equals(INSTANCE.version);
  }

}
