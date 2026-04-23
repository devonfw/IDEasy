package com.devonfw.tools.ide.os;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Mock implementation of {@link WindowsHelper} for testing.
 */
public class WindowsHelperMock extends WindowsHelperImpl {

  private final Properties env;

  private final Map<String, Map<String, String>> fakeRegistry = new HashMap<>();

  /**
   * The constructor.
   */
  public WindowsHelperMock(IdeContext context) {

    super(context);
    this.env = new Properties();
    this.env.setProperty("IDE_ROOT", "C:\\projects");
    this.env.setProperty("PATH",
        "C:\\Users\\testuser\\AppData\\Local\\Microsoft\\WindowsApps;C:\\projects\\_ide\\installation\\bin;C:\\Users\\testuser\\scoop\\apps\\python\\current\\Scripts;C:\\Users\\testuser\\scoop\\apps\\python\\current;C:\\Users\\testuser\\scoop\\shims");
    Map<String, String> gitUninstall = new HashMap<>();
    gitUninstall.put("DisplayName", "Git");
    gitUninstall.put("UninstallString", "\"C:\\Users\\testuser\\Git\\unins000.exe\"");

    this.fakeRegistry.put(
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Git_is1",
        gitUninstall
    );
  }

  @Override
  public void setUserEnvironmentValue(String key, String value) {

    this.env.setProperty(key, value);
  }

  @Override
  public void removeUserEnvironmentValue(String key) {

    this.env.remove(key);
  }

  @Override
  public String getUserEnvironmentValue(String key) {

    return this.env.getProperty(key);
  }

  @Override
  public String getRegistryValue(String path, String key) {

    if (WindowsHelperImpl.HKCU_ENVIRONMENT.equals(path)) {
      return getUserEnvironmentValue(key);
    } else if (path.contains("GitForWindows")) {
      return super.getRegistryValue(path, key);
    }
    return null;
  }

  @Override
  public String getRegistryValueBySearch(String displayNameRegex, String key) {

    Pattern pattern = Pattern.compile(displayNameRegex, Pattern.CASE_INSENSITIVE);
    for (Map<String, String> values : this.fakeRegistry.values()) {
      String displayName = values.get("DisplayName");
      if (displayName != null && pattern.matcher(displayName).find()) {
        return values.get(key);
      }
    }
    return null;
  }
}
