package com.devonfw.tools.ide.expression.path;

import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * {@link Enum} with available modes to format a path.
 *
 * @see PathFunction
 */
public enum PathMode {

  /**
   * Format path in unix/posix syntax with slashes.
   */
  UNIX() {
    @Override
    public String format(String path, SystemInfo systemInfo) {

      return path.replace('\\', '/');
    }
  },

  /**
   * Format path in OS native syntax depending on the current OS.
   */
  NATIVE() {
    @Override
    public String format(String path, SystemInfo systemInfo) {

      if (systemInfo.isWindows()) {
        return WindowsPathSyntax.WINDOWS.normalize(path).replace('/', '\\');
      }
      return path.replace('\\', '/');
    }
  };

  /**
   * @param path the path to format.
   * @param systemInfo the {@link SystemInfo}.
   * @return the formatted {@code path}.
   */
  public abstract String format(String path, SystemInfo systemInfo);
}
