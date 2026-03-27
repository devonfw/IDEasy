package com.devonfw.tools.ide.os;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Implementation of {@link SystemInfo}.
 */
public class SystemInfoImpl implements SystemInfo {

  /** The default {@link SystemInfo} instance for the operating system running this program. */
  public static final SystemInfo INSTANCE = new SystemInfoImpl();

  /** Name of {@link System#getProperty(String) system-property} for {@link #getOsName()}: {@value}. */
  private static final String PROPERTY_OS_NAME = "os.name";

  /** Name of {@link System#getProperty(String) system-property} for #getArchitectureName()}: {@value}. */
  private static final String PROPERTY_OS_ARCHITECTURE = "os.arch";

  /** Name of {@link System#getProperty(String) system-property} for {@link #getOsVersion()}: {@value}. */
  private static final String PROPERTY_OS_VERSION = "os.version";

  private final String osName;

  private final VersionIdentifier osVersion;

  private final OperatingSystem os;

  private final String architectureName;

  private final SystemArchitecture architecture;

  /**
   * The constructor.
   */
  private SystemInfoImpl() {

    this(System.getProperty(PROPERTY_OS_NAME).trim(), System.getProperty(PROPERTY_OS_VERSION).trim(),
        resolveArchitecture());
  }

  private static String resolveArchitecture() {

    String arch = System.getProperty(PROPERTY_OS_ARCHITECTURE).trim();
    String osName = System.getProperty(PROPERTY_OS_NAME).trim().toLowerCase(java.util.Locale.ROOT);
    if (!osName.startsWith("windows")) {
      try {
        Process process = new ProcessBuilder("uname", "-m").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line = reader.readLine();
          if (line != null && !line.isBlank()) {
            arch = line.trim();
          }
        }
      } catch (Exception e) {
        // fallback to system property
      }
    }
    return arch;
  }

  /**
   * The constructor.
   *
   * @param osName the {@link #getOsName() OS name}
   * @param osVersion the {@link #getOsVersion() OS version}.
   * @param architectureName the {@link #getArchitectureName() architecture name}.
   */
  public SystemInfoImpl(String osName, String osVersion, String architectureName) {

    super();
    this.osName = osName;
    this.osVersion = VersionIdentifier.of(osVersion);
    this.architectureName = architectureName;
    this.os = OperatingSystem.ofName(this.osName);
    this.architecture = detectArchitecture(this.architectureName);
  }

  private static SystemArchitecture detectArchitecture(String architectureName) {

    if (architectureName.contains("arm") || architectureName.contains("aarch")) {
      return SystemArchitecture.ARM64;
    } else {
      return SystemArchitecture.X64;
    }
  }

  @Override
  public OperatingSystem getOs() {

    return this.os;
  }

  @Override
  public String getOsName() {

    return this.osName;
  }

  @Override
  public VersionIdentifier getOsVersion() {

    return this.osVersion;
  }

  @Override
  public String getArchitectureName() {

    return this.architectureName;
  }

  @Override
  public SystemArchitecture getArchitecture() {

    return this.architecture;
  }

  @Override
  public String toString() {

    return this.os + "@" + this.architecture + "(" + this.osName + "[" + this.osVersion + "]@" + this.architectureName
        + ")";
  }

}
