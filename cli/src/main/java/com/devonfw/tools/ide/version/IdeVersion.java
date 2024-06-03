package com.devonfw.tools.ide.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Class to {@link #get()} the current version of this IDE product.
 */
public final class IdeVersion {

  private static final IdeVersion INSTANCE = new IdeVersion();

  private final String version;

  // most simple solution would be maven filtering but that is kind of tricky for java files
  // http://www.mojohaus.org/templating-maven-plugin/examples/source-filtering.html
  // private static final String VERSION = "${project.version}";

  private IdeVersion() {

    super();
    String v = getClass().getPackage().getImplementationVersion();
    if (v == null) {
      v = "SNAPSHOT";
    }
    this.version = v;
  }

  private String getValue(Manifest manifest, Name name) {

    return manifest.getMainAttributes().getValue(name);
  }

  /**
   * @return the current version of this IDE product.
   */
  public static String get() {

    // return VERSION;
    return INSTANCE.version;
  }

}
