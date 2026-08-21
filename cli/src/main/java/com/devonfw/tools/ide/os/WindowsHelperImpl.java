package com.devonfw.tools.ide.os;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Implementation of {@link WindowsHelper}.
 */
public class WindowsHelperImpl implements WindowsHelper {

  private static final Logger LOG = LoggerFactory.getLogger(WindowsHelperImpl.class);

  /** Registry key for the users environment variables. */
  public static final String HKCU_ENVIRONMENT = "HKCU\\Environment";

  public static final String POWERSHELL_CODE_SOURCE_FUNCTIONS =
      ". \"$env:IDE_ROOT\\_ide\\installation\\functions.ps1\"";

  /** Common Windows registry base paths containing (uninstall) information for installed applications (system-wide and per-user). */
  private static final String[] REGISTRY_BASE_PATHS = {
      "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
      "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
      "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
  };

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public WindowsHelperImpl(IdeContext context) {

    this.context = context;
  }

  @Override
  public void setUserEnvironmentValue(String key, String value) {

    ProcessResult result = this.context.newProcess().executable("setx").addArgs(key, value).run(ProcessMode.DEFAULT_SILENT);
    assert (result.isSuccessful());
  }

  @Override
  public void removeUserEnvironmentValue(String key) {
    ProcessResult result = this.context.newProcess().executable("reg").addArgs("delete", HKCU_ENVIRONMENT, "/v", key, "/f")
        .errorHandling(ProcessErrorHandling.LOG_WARNING).run(ProcessMode.DEFAULT_CAPTURE);
    if (result.isSuccessful()) {
      LOG.debug("Removed environment variable {}", key);
    } else {
      result.log(IdeLogLevel.WARNING);
    }
  }

  @Override
  public String getUserEnvironmentValue(String key) {

    return getRegistryValue(HKCU_ENVIRONMENT, key);
  }

  @Override
  public String getRegistryValue(String path, String key) {

    List<String> out = runReg("query", path, "/v", key);
    if (out != null) {
      return retrieveRegString(key, out);
    }
    return null;
  }

  @Override
  public WindowsAppInstallation getAppInstallationFromRegistry(String appName) {
    String uninstallKey = findUninstallKey(appName);
    if (uninstallKey == null) {
      return null;
    }

    List<String> out = runReg("query", uninstallKey);
    if (out == null) {
      return null;
    }

    String version = retrieveRegString("DisplayVersion", out);
    String icon = retrieveRegString("DisplayIcon", out);
    String uninstallString = retrieveRegString("UninstallString", out);
    String installLocation = retrieveRegString("InstallLocation", out);

    return new WindowsAppInstallation(version, icon, uninstallString, installLocation);
  }

  @Override
  public void configurePowerShellProfiles(boolean install) {

    if (!this.context.getSystemInfo().isWindows()) {
      return;
    }

    // Windows PowerShell 5.x and PowerShell 7+ have different profile locations.
    modifyPowerShellProfile("powershell", install);
    modifyPowerShellProfile("pwsh", install);
  }

  private String findUninstallKey(String appName) {

    for (String registryBasePath : REGISTRY_BASE_PATHS) {
      List<String> out = runReg("query", registryBasePath, "/s", "/f", appName);
      if (out == null) {
        continue;
      }
      for (String line : out) {
        line = line.trim();
        if (line.startsWith("HKEY_")) {
          return line; // exact registry path (key) for tool
        }
      }
    }
    return null;
  }

  /**
   * Executes a Windows registry command and returns its output.
   *
   * @param args the registry command arguments.
   * @return the command output lines, or {@code null} if the command failed
   */
  protected List<String> runReg(String... args) {
    ProcessResult result = this.context.newProcess()
        .errorHandling(ProcessErrorHandling.LOG_WARNING)
        .executable("reg")
        .addArgs(args)
        .run(ProcessMode.DEFAULT_CAPTURE);
    if (!result.isSuccessful()) {
      return null;
    }
    return result.getOut();
  }

  /**
   * Parses the result of a registry query and outputs the given key.
   *
   * @param key the key to look for.
   * @param out List of keys from registry query result.
   * @return the registry value.
   */
  protected String retrieveRegString(String key, List<String> out) {
    for (String line : out) {
      int i = line.indexOf(key);
      if (i >= 0) {
        assert (i == 4);
        i += key.length();
        i = skipWhitespaces(line, i);
        i = skipNonWhitespaces(line, i); // the type (e.g. "REG_SZ")
        i = skipWhitespaces(line, i);
        line = line.substring(i);
        return line;
      }
    }
    return null;
  }

  private static int skipWhitespaces(String string, int i) {

    int len = string.length();
    while ((i < len) && Character.isWhitespace(string.charAt(i))) {
      i++;
    }
    return i;
  }

  private static int skipNonWhitespaces(String string, int i) {

    int len = string.length();
    while ((i < len) && !Character.isWhitespace(string.charAt(i))) {
      i++;
    }
    return i;
  }

  private void modifyPowerShellProfile(String executable, boolean install) {

    Path profilePath = getPowerShellProfilePath(executable);
    if (profilePath == null) {
      return;
    }

    String action = install ? "Configuring" : "Removing";
    LOG.info("{} IDEasy in {}", action, profilePath);

    FileAccess fileAccess = this.context.getFileAccess();
    List<String> lines = fileAccess.readFileLines(profilePath);

    if (lines == null && !install) {
      return;
    }

    List<String> modifiedLines = modifyPowerShellProfileLines(lines, install);

    Path parent = profilePath.getParent();
    if (parent != null) {
      fileAccess.mkdirs(parent);
    }

    fileAccess.writeFileLines(modifiedLines, profilePath);
    LOG.debug("Successfully updated PowerShell profile {}", profilePath);
  }

  private Path getPowerShellProfilePath(String executable) {

    try {
      ProcessResult result = this.context.newProcess()
          .executable(executable)
          .addArgs("-NoProfile", "-Command", "$PROFILE.CurrentUserAllHosts")
          .run(ProcessMode.DEFAULT_CAPTURE);

      if (!result.isSuccessful()) {
        LOG.debug("{} is not available or its profile could not be determined.", executable);
        return null;
      }

      List<String> output = result.getOut();
      if (output == null || output.isEmpty()) {
        LOG.debug("{} returned no PowerShell profile path.", executable);
        return null;
      }

      String profilePath = output.stream()
          .map(String::strip)
          .filter(line -> !line.isEmpty())
          .findFirst()
          .orElse(null);

      if (profilePath == null) {
        LOG.debug("{} returned no PowerShell profile path.", executable);
        return null;
      }

      return Path.of(profilePath);

    } catch (Exception e) {
      // pwsh is optional. Windows PowerShell normally exists on supported Windows versions.
      LOG.debug("Could not determine profile for {}: {}", executable, e.getMessage());
      return null;
    }
  }

  List<String> modifyPowerShellProfileLines(List<String> lines, boolean install) {

    List<String> modifiedLines;

    if (lines == null) {
      modifiedLines = new ArrayList<>();
    } else {
      modifiedLines = new ArrayList<>(lines);
    }

    boolean configured = modifiedLines.stream()
        .map(String::trim)
        .anyMatch(POWERSHELL_CODE_SOURCE_FUNCTIONS::equals);

    if (install) {
      if (!configured) {
        modifiedLines.add(POWERSHELL_CODE_SOURCE_FUNCTIONS);
      }
    } else {
      modifiedLines.removeIf(
          line -> line.trim().equals(POWERSHELL_CODE_SOURCE_FUNCTIONS));
    }

    return modifiedLines;
  }
}
