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
    Manifest manifest = loadManifest(IdeVersion.class);
    String v = null;
    if (manifest != null) {
      v = getValue(manifest, Attributes.Name.IMPLEMENTATION_VERSION);
      if (v == null) {
        v = getValue(manifest, Attributes.Name.SPECIFICATION_VERSION);
      }
    }
    if (v == null) {
      v = "SNAPSHOT";
    }
    this.version = v;
  }

  private String getValue(Manifest manifest, Name name) {

    return manifest.getMainAttributes().getValue(name);
  }

  /**
   * This method tries to load the {@link Manifest} for the given {@link Class}. E.g. if the given {@link Class} comes
   * from a {@link JarFile} the {@link JarFile#getManifest() Manifest} of that {@link JarFile} is returned.
   *
   * @param type is the {@link Class} for which the according {@link Manifest} is requested.
   * @return the according {@link Manifest} or {@code null} if NOT available.
   */
  private static Manifest loadManifest(Class<?> type) {

    try {
      Manifest manifest = null;
      String classFile = type.getName().replace('.', '/') + ".class";
      URL classUrl = type.getClassLoader().getResource(classFile);
      if (classUrl != null) {
        String protocol = classUrl.getProtocol().toLowerCase(Locale.US);
        if ("jar".equals(protocol)) {
          JarURLConnection connection = (JarURLConnection) classUrl.openConnection();
          JarFile jarFile = connection.getJarFile();
          // create a copy to avoid modification of the original manifest...
          manifest = jarFile.getManifest();
        } else if ("file".equals(protocol)) {
          try {
            Path classPath = Path.of(classUrl.toURI());
            Path manifestPath = classPath;
            int slashIndex = 0;
            while (slashIndex >= 0) {
              slashIndex = classFile.indexOf('/', slashIndex + 1);
              manifestPath = manifestPath.getParent();
            }
            manifestPath = manifestPath.resolve(JarFile.MANIFEST_NAME);
            if (Files.exists(manifestPath)) {
              try (InputStream inputStream = Files.newInputStream(manifestPath)) {
                manifest = new Manifest(inputStream);
              }
            }
          } catch (URISyntaxException e) {
            // ignore
          }
        }
      }
      return manifest;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read manifest.", e);
    }
  }

  /**
   * @return the current version of this IDE product.
   */
  public static String get() {

    // return VERSION;
    return INSTANCE.version;
  }

}
