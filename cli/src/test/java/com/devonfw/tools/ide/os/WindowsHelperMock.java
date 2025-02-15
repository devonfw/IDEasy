package com.devonfw.tools.ide.os;

import java.util.Properties;

/**
 * Mock implementation of {@link WindowsHelper} for testing.
 */
public class WindowsHelperMock implements WindowsHelper {

  private final Properties env;

  /**
   * The constructor.
   */
  public WindowsHelperMock() {

    super();
    this.env = new Properties();
    this.env.setProperty("IDE_ROOT", "C:\\projects");
    this.env.setProperty("PATH",
        "C:\\Users\\testuser\\AppData\\Local\\Microsoft\\WindowsApps;C:\\projects\\_ide\\installation\\bin;C:\\Users\\testuser\\scoop\\apps\\python\\current\\Scripts;C:\\Users\\testuser\\scoop\\apps\\python\\current;C:\\Users\\testuser\\scoop\\shims");
  }

  @Override
  public void setUserEnvironmentValue(String key, String value) {

    this.env.setProperty(key, value);
  }

  @Override
  public String getUserEnvironmentValue(String key) {

    return this.env.getProperty(key);
  }

  @Override
  public String getRegistryValue(String path, String key) {

    if (WindowsHelperImpl.HKCU_ENVIRONMENT.equals(path)) {
      return getUserEnvironmentValue(key);
    }
    return null;
  }
}
