package com.devonfw.tools.ide.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utility class for reading selected information from the Windows Registry using the native {@code reg.exe} command-line tool.
 */
public final class WindowsRegistryUtil {

  private WindowsRegistryUtil() {
    // utility class
  }

  /** Uninstall registry keys scanned to locate installed applications. */
  private static final String[] UNINSTALL_REGISTRY_KEYS = {
      "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
      "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
      "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
  };

  /**
   * Resolves the {@code DisplayVersion} of an installed application.
   *
   * <p>
   * The application is located by searching all standard {@code Uninstall} registry keys and matching the {@code DisplayName} against the given regular
   * expression.
   * </p>
   *
   * @param displayNameRegex regular expression used to match the {@code DisplayName} (case-insensitive, e.g. {@code "pgAdmin 4"})
   * @return {@link Optional} containing the display version if the application is found, or {@link Optional#empty()} otherwise
   */
  public static Optional<String> getDisplayVersion(String displayNameRegex) {
    return findValue(displayNameRegex, "DisplayVersion");
  }

  /**
   * Resolves the uninstall executable path of an installed application.
   *
   * <p>
   * The application is located by searching all standard {@code Uninstall} registry keys and matching the {@code DisplayName} against the given regular
   * expression. Any command-line arguments are removed so that the returned value represents a path-compatible executable.
   * </p>
   *
   * @param displayNameRegex regular expression to match the applications's {@code DisplayName}
   * @return {@link Optional} containing the uninstall executable path, or {@link Optional#empty()} if the application is not found
   */
  public static Optional<String> getUninstallString(String displayNameRegex) {
    return findValue(displayNameRegex, "UninstallString")
        .map(s -> {
          String value = s.trim();

          while (value.startsWith("\"\"") && value.endsWith("\"\"")) {
            value = value.substring(1, value.length() - 1).trim();
          }

          if (value.startsWith("\"")) {
            int end = value.indexOf('"', 1);
            if (end > 1) {
              return value.substring(1, end);
            }
          }

          int space = value.indexOf(' ');
          return space > 0 ? value.substring(0, space) : value;
        });
  }

  private static Optional<String> findValue(String displayNameRegex, String valueName) {
    Pattern pattern = Pattern.compile(displayNameRegex, Pattern.CASE_INSENSITIVE);

    for (String baseKey : UNINSTALL_REGISTRY_KEYS) {
      try {
        List<String> output = runReg("query", baseKey, "/s");
        String currentKey = null;

        for (String line : output) {
          line = line.trim();

          if (line.startsWith("HKEY_")) {
            currentKey = line;
            continue;
          }

          if (currentKey != null && line.startsWith("DisplayName")) {
            String name = extractRegistryValue(line);
            if (name != null && pattern.matcher(name).find()) {
              return readValue(currentKey, valueName);
            }
          }
        }
      } catch (Exception e) {
        // ignore
      }
    }

    return Optional.empty();
  }

  private static Optional<String> readValue(String key, String valueName) {
    try {
      for (String line : runReg("query", key, "/v", valueName)) {
        line = line.trim();
        if (line.startsWith(valueName)) {
          return Optional.ofNullable(extractRegistryValue(line));
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return Optional.empty();
  }

  private static List<String> runReg(String... args) throws Exception {
    List<String> command = new ArrayList<>();
    command.add("reg");
    command.addAll(Arrays.asList(args));

    Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start();

    List<String> lines = new ArrayList<>();
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  /**
   * Extrahiert den Registry-Wert korrekt (REG_SZ, REG_EXPAND_SZ, etc.) Unterstützt Werte mit Leerzeichen.
   */
  private static String extractRegistryValue(String line) {
    int idx = line.indexOf("REG_");
    if (idx < 0) {
      return null;
    }
    return line.substring(idx)
        .replaceFirst("REG_\\w+\\s+", "")
        .trim();
  }
}
