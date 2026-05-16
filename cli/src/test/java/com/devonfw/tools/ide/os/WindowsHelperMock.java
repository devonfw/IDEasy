package com.devonfw.tools.ide.os;

import java.util.List;
import java.util.Properties;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Mock implementation of {@link WindowsHelper} for testing.
 */
public class WindowsHelperMock extends WindowsHelperImpl {

  private final Properties env;

  private static final String MOCK_APP_NAME = "TestApp";

  private static final String MOCK_DISPLAY_VERSION = "1.1.1";

  private static final String MOCK_INSTALL_LOCATION = "C:\\Program Files\\TestApp";

  private static final String MOCK_DISPLAY_ICON =
      "C:\\Program Files\\TestApp\\testapp.exe,0";
  private static final String MOCK_UNINSTALL_STRING =
      "\"C:\\Program Files\\TestApp\\uninstall.exe\"";


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
  public String getDisplayVersionFromRegistry(String appName) {
    return matchesApp(appName) ? MOCK_DISPLAY_VERSION : null;
  }

  @Override
  public String getDisplayIconFromRegistry(String appName) {
    return matchesApp(appName) ? MOCK_DISPLAY_ICON : null;
  }

  @Override
  public String getUninstallStringFromRegistry(String appName) {
    return matchesApp(appName) ? MOCK_UNINSTALL_STRING : null;
  }

  @Override
  public String getInstallLocationFromRegistry(String appName) {
    return matchesApp(appName) ? MOCK_INSTALL_LOCATION : null;
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

  private boolean matchesApp(String appName) {
    return appName != null
        && appName.equalsIgnoreCase(MOCK_APP_NAME);
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
