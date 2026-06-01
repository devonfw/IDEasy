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
