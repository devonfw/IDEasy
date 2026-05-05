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

  private static final String MOCK_DISPLAY_NAME = "Test Application";

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
  public String getDisplayNameFromRegistry(String appName) {
    return matchesApp(appName) ? MOCK_DISPLAY_NAME : null;
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

    // simulate reg.exe filtering: "/f <appName>"
    String searchValue = null;
    for (int i = 0; i < args.length - 1; i++) {
      if ("/f".equalsIgnoreCase(args[i])) {
        searchValue = args[i + 1];
        break;
      }
    }

    // Only return output if searched app matches
    if (!MOCK_APP_NAME .equalsIgnoreCase(searchValue)) {
      return List.of(); // same behavior as reg.exe: no results
    }

    return List.of(
        "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\TestApp",
        "    DisplayName    REG_SZ    Test Application",
        "    DisplayIcon    REG_SZ    C:\\Program Files\\TestApp\\testapp.exe,0",
        "    InstallLocation    REG_SZ    C:\\Program Files\\TestApp",
        "    UninstallString    REG_SZ    \"C:\\Program Files\\TestApp\\uninstall.exe\""
    );
  }
}
