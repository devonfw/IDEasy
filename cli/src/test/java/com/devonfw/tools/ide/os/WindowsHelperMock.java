package com.devonfw.tools.ide.os;

import java.util.Properties;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Mock implementation of {@link WindowsHelper} for testing.
 */
public class WindowsHelperMock extends WindowsHelperImpl {

  private final Properties env;

  /**
   * The constructor.
   */
  public WindowsHelperMock(IdeContext context) {

    super(context);
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
}
