package com.devonfw.tools.ide.cli;

import java.nio.file.Path;

import org.graalvm.nativeimage.ProcessProperties;

/**
 * Helper class for GraalVM.
 */
public class GraalVmHelper {

  private static final GraalVmHelper INSTANCE = new GraalVmHelper();

  private final boolean nativeImage;

  private GraalVmHelper() {

    super();
    String classPath = System.getProperty("java.class.path");
    this.nativeImage = ((classPath == null) || classPath.isBlank());
  }

  /**
   * @return {@code true} if IDEasy is running from a GraalVM native image, {@code false} otherwise ({@link #isJvm() running in JVM}).
   */
  public boolean isNativeImage() {

    return this.nativeImage;
  }

  /**
   * @return {@code true} if IDEasy is running in Java virtual machine, {@code false} otherwise ({@link #isNativeImage() running from native-image}.
   */
  public boolean isJvm() {

    return !this.nativeImage;
  }

  public Path getCwd() {

    Path cwd;
    if (this.nativeImage) {
      String executableName = ProcessProperties.getExecutableName();
      Path ideasyBinaryPath = Path.of(executableName).toAbsolutePath();
      Path binPath = ideasyBinaryPath.getParent();
      if (!binPath.getFileName().toString().equals("bin")) {
        System.out.println("WARNING: Expected native image binary to be in bin path but found " + ideasyBinaryPath);
      }
      cwd = binPath.getParent();
    } else {
      cwd = Path.of("").toAbsolutePath();
    }
    return cwd;
  }

  /**
   * @return the singleton instance of {@link GraalVmHelper}.
   */
  public static GraalVmHelper get() {

    return INSTANCE;
  }
}
