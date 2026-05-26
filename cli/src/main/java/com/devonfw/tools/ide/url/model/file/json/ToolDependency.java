package com.devonfw.tools.ide.url.model.file.json;

import com.devonfw.tools.ide.json.JsonObject;
import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * Model to represent a dependency of a tool (inside a "dependencies.json" file).
 * <p>
 * Extended with optional OS / architecture so dependencies can be targeted to specific platforms.
 */
public record ToolDependency(String tool, VersionRange versionRange, OperatingSystem os, SystemArchitecture arch)
    implements JsonObject {

  /**
   * Convenience constructor for tests and existing callers that only provided tool and versionRange.
   */
  public ToolDependency(String tool, VersionRange versionRange) {
    this(tool, versionRange, null, null);
  }


  /**
   * @param systemInfo the {@link SystemInfo} describing the current system.
   * @return {@code true} if this dependency applies to the given system (os/arch), {@code false} otherwise.
   */
  public boolean appliesTo(SystemInfo systemInfo) {
    if (this.os != null && this.os != systemInfo.getOs()) {
      return false;
    }
    return this.arch == null || this.arch == systemInfo.getArchitecture();
  }

}
