package com.devonfw.tools.ide.os;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Implementation of {@link WindowsHelper}.
 */
public class WindowsHelperImpl implements WindowsHelper {

  /** Registry key for the users environment variables. */
  public static final String HKCU_ENVIRONMENT = "HKCU\\Environment";

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
      this.context.debug("Removed environment variable {}", key);
    } else {
      result.log(IdeLogLevel.WARNING, this.context);
    }
  }

  @Override
  public String getUserEnvironmentValue(String key) {

    return getRegistryValue(HKCU_ENVIRONMENT, key);
  }

  @Override
  public String getRegistryValue(String path, String key) {

    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable("reg").addArgs("query", path, "/v", key)
        .run(ProcessMode.DEFAULT_CAPTURE);
    List<String> out = result.getOut();
    if (!result.isSuccessful()) {
      return null;
    }
    return retrieveRegString(key, out);
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

}
