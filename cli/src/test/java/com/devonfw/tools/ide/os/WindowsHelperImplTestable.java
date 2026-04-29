package com.devonfw.tools.ide.os;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Test-specific subclass of {@link WindowsHelperImpl}.
 *
 * <p>
 * Mainly used as a test seam to simulate the reg.exe command for test purposes.
 * </p>
 */
public class WindowsHelperImplTestable extends WindowsHelperImpl {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public WindowsHelperImplTestable(IdeContext context) {

    super(context);
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
    if (!"TestApp".equalsIgnoreCase(searchValue)) {
      return List.of();
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
