package com.devonfw.tools.ide.migration.v2025;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.migration.IdeVersionMigration;
import com.devonfw.tools.ide.os.WindowsHelper;

/**
 * Migration for 2025.02.001-beta. Removes old entries of IDEasy without "installation" folder from Windows PATH and old entries from .bashrc and .zshrc.
 */
public class Mig202502001 extends IdeVersionMigration {

  /**
   * The constructor.
   */
  public Mig202502001() {

    super("2025.02.001-beta");
  }

  @Override
  public void run(IdeContext context) {

    cleanupBashAndZshRc(context);
    cleanupWindowsPath(context);
  }

  private static void cleanupBashAndZshRc(IdeContext context) {

    cleanupShellRc(context, ".bashrc");
    cleanupShellRc(context, ".zshrc");
  }

  private static void cleanupShellRc(IdeContext context, String filename) {

    Path rcFile = context.getUserHome().resolve(filename);
    FileAccess fileAccess = context.getFileAccess();
    List<String> lines = fileAccess.readFileLines(rcFile);
    if (lines == null) {
      return;
    }
    // since it is unspecified if the returned List may be immutable we want to get sure
    lines = new ArrayList<>(lines);
    Iterator<String> iterator = lines.iterator();
    int removeCount = 0;
    while (iterator.hasNext()) {
      String line = iterator.next();
      if (isObsoleteRcLine(line.trim())) {
        context.info("Removing obsolete line from {}: {}", filename, line);
        iterator.remove();
        removeCount++;
      }
    }
    if (removeCount > 0) {
      fileAccess.writeFileLines(lines, rcFile);
    }
  }

  private static boolean isObsoleteRcLine(String line) {
    if (line.startsWith("alias ide=")) {
      return true;
    } else if (line.equals("ide")) {
      return true;
    } else if (line.equals("ide init")) {
      return true;
    } else if (line.equals("source \"$IDE_ROOT/_ide/functions\"")) {
      return true;
    }
    return false;
  }

  private static void cleanupWindowsPath(IdeContext context) {
    if (context.getSystemInfo().isWindows()) {
      WindowsHelper helper = WindowsHelper.get(context);
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
