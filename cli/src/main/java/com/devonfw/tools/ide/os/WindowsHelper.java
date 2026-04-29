package com.devonfw.tools.ide.os;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Internal helper for Windows workarounds.
 */
public interface WindowsHelper {

  /**
   * @param key the name of the environment variable.
   * @param value the new value of the environment variable.
   */
  void setUserEnvironmentValue(String key, String value);

  /**
   * @param key the name of the environment variable to remove.
   */
  void removeUserEnvironmentValue(String key);

  /**
   * @param key the name of the environment variable.
   * @return the value of the environment variable in the users context.
   */
  String getUserEnvironmentValue(String key);

  /**
   * @param path the path in the Windows registry.
   * @param key the key in the Windows registry.
   * @return the value from the Windows registry for the given arguments or {@code null} if no such entry was found.
   */
  String getRegistryValue(String path, String key);

  /**
   * @param appName the application name to search for in the Windows registry.
   * @return the DisplayName entry if the application is found in the Windows registry or {@code null} if nothing was found.
   */
  String getDisplayNameFromRegistry(String appName);

  /**
   * @param appName the application name to search for in the Windows registry.
   * @return the DisplayIcon entry if the application is found in the Windows registry or {@code null} if nothing was found.
   */
  String getDisplayIconFromRegistry(String appName);

  /**
   * @param appName the application name to search for in the Windows registry.
   * @return the UninstallString entry if the application is found in the Windows registry or {@code null} if nothing was found.
   */
  String getUninstallStringFromRegistry(String appName);

  /**
   * @param appName the application name to search for in the Windows registry.
   * @return the InstallLocation entry if the application is found in the Windows registry or {@code null} if nothing was found.
   */
  String getInstallLocationFromRegistry(String appName);

  /**
   * @param context the {@link IdeContext}.
   * @return the instance of {@link WindowsHelper}.
   */
  static WindowsHelper get(IdeContext context) {
    // IdeContext API is already too large
    return ((AbstractIdeContext) context).getWindowsHelper();
  }
}
