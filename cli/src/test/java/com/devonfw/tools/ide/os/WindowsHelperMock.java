package com.devonfw.tools.ide.os;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Mock implementation of {@link WindowsHelper} for testing.
 */
public class WindowsHelperMock extends WindowsHelperImpl {

  private final Properties env;

  /** Mock registry map storing WindowsAppInstallation entries */
  private final Map<String, WindowsAppInstallation> registry;

  /**
   * The constructor.
   */
  public WindowsHelperMock(IdeContext context) {

    super(context);
    this.env = new Properties();
    this.env.setProperty("IDE_ROOT", "C:\\projects");
    this.env.setProperty("PATH",
        "C:\\Users\\testuser\\AppData\\Local\\Microsoft\\WindowsApps;C:\\projects\\_ide\\installation\\bin;C:\\Users\\testuser\\scoop\\apps\\python\\current\\Scripts;C:\\Users\\testuser\\scoop\\apps\\python\\current;C:\\Users\\testuser\\scoop\\shims");
    this.registry = new HashMap<>();
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

  /**
   * Set full installation info for an app in the mock registry. This allows to test the retrieval of all relevant information for an installed app from the
   * registry.
   *
   * @param appName the name of the app to set in the registry
   * @param installation the installation info to set
   */
  public void setAppInstallationFromRegistry(String appName, WindowsAppInstallation installation) {
    this.registry.put(appName, installation);
  }

  @Override
  public WindowsAppInstallation getAppInstallationFromRegistry(String appName) {
    if (appName == null) {
      return null;
    }
    return this.registry.get(appName);
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
  protected List<String> runReg(String... args) {

    String searchValue = extractFilterValue(args);
    // Case: reg query <basePath> /s /f <appName>
    if (searchValue != null) {
      if (!"TestApp".equalsIgnoreCase(searchValue)) {
        return List.of();
      }
      return List.of(
          "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\TestApp",
          "    DisplayName    REG_SZ    TestApp"
      );
    }
    // Case: reg query <exactKey>
    if (args.length >= 2 &&
        args[0].equalsIgnoreCase("query") &&
        args[1].endsWith("\\Uninstall\\TestApp")) {

      return List.of(
          "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\TestApp",
          "    DisplayName    REG_SZ    TestApp",
          "    DisplayVersion    REG_SZ    1.1.1",
          "    DisplayIcon    REG_SZ    C:\\Program Files\\TestApp\\testapp.exe,0",
          "    InstallLocation    REG_SZ    C:\\Program Files\\TestApp",
          "    UninstallString    REG_SZ    \"C:\\Program Files\\TestApp\\uninstall.exe\""
      );
    }

    return List.of();
  }

  private static String extractFilterValue(String[] args) {

    for (int i = 0; i < args.length - 1; i++) {
      if ("/f".equalsIgnoreCase(args[i])) {
        return args[i + 1];
      }
    }
    return null;
  }
}
