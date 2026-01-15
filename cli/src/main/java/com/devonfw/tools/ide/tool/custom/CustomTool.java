package com.devonfw.tools.ide.tool.custom;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.os.OperatingSystem;

/**
 * JSON representation of a single {@link CustomToolMetadata}.
 *
 * @param name the {@link CustomToolMetadata#getTool() tool name}.
 * @param version the {@link CustomToolMetadata#getVersion() tool version}.
 * @param osAgnostic {@code true} if {@link OperatingSystem} agnostic, {@code false} otherwise.
 * @param archAgnostic {@code true} if {@link com.devonfw.tools.ide.os.SystemArchitecture} agnostic, {@code false} otherwise.
 * @param url the overridden {@link CustomTools#url() repository URL} or {@code null} to inherit.
 * @see CustomTools#tools()
 */
public record CustomTool(String name, String version, boolean osAgnostic, boolean archAgnostic, String url) implements JsonObject {

  /** JSON property name for {@link #name()}. */
  public static final String PROPERTY_NAME = "name";

  /** JSON property name for {@link #version()}. */
  public static final String PROPERTY_VERSION = "version";

  /** JSON property name for {@link #osAgnostic()}. */
  public static final String PROPERTY_OS_AGNOSTIC = "os-agnostic";

  /** JSON property name for {@link #archAgnostic()}. */
  public static final String PROPERTY_ARCH_AGNOSTIC = "arch-agnostic";

  /** JSON property name for {@link #url()}. */
  public static final String PROPERTY_URL = "url";

  /**
   * @return a new {@link CustomTools} having {@link #url()} set to {@code null}.
   */
  public CustomTool withoutUrl() {

    return new CustomTool(name, version, osAgnostic, archAgnostic, null);
  }
}
