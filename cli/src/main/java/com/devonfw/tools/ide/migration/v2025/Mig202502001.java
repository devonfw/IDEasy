package com.devonfw.tools.ide.migration.v2025;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.IdeVersionMigration;
import com.devonfw.tools.ide.os.WindowsHelper;

public class Mig202502001 extends IdeVersionMigration {

  public Mig202502001() {

    super("2025.02.001-beta");
  }

  @Override
  public void run(IdeContext context) {

    if (context.getSystemInfo().isWindows()) {
      WindowsHelper helper = new WindowsHelper(context);
      String userPath = helper.getUserEnvironmentValue("PATH");
      if (userPath == null) {
        context.warning("Could not read user PATH from registry!");
      } else {
        context.trace("Found user PATH={}", userPath);
        userPath = removeObsoleteEntryFromWindowsPath(userPath);
        if (userPath != null) {
          helper.setUserEnvironmentValue("PATH", userPath);
        }
      }
    }
  }

  static String removeObsoleteEntryFromWindowsPath(String userPath) {
    int len = userPath.length();
    int start = 0;
    while ((start >= 0) && (start < len)) {
      int end = userPath.indexOf(';', start);
      if (end < 0) {
        end = len;
      }
      String entry = userPath.substring(start, end);
      if (entry.endsWith("\\_ide\\bin")) {
        String prefix = "";
        int offset = 1;
        if (start > 0) {
          prefix = userPath.substring(0, start - 1);
          offset = 0;
        }
        if (end == len) {
          return prefix;
        } else {
          return prefix + userPath.substring(end + offset);
        }
      }
      start = end + 1;
    }
    return null;
  }

}
