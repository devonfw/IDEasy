package com.devonfw.tools.ide.os;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Internal helper class for Windows workarounds.
 */
public class WindowsHelper {

  private final IdeContext context;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public WindowsHelper(IdeContext context) {

    this.context = context;
  }

  /**
   * @param key the name of the environment variable.
   * @param value the new value of the environment variable.
   */
  public void setUserEnvironmentValue(String key, String value) {

    ProcessResult result = this.context.newProcess().executable("setx").addArgs(key, value).run(ProcessMode.DEFAULT_SILENT);
    assert (result.isSuccessful());
  }

  /**
   * @param key the name of the environment variable.
   * @return the value of the environment variable in the users context.
   */
  public String getUserEnvironmentValue(String key) {

    return getRegistryValue("HKCU\\Environment", key);
  }

  /**
   * @param path the path in the Windows registry.
   * @param key the key in the Windows registry.
   * @return the value from the Windows registry for the given arguments or {@code null} if no such entry was found.
   */
  public String getRegistryValue(String path, String key) {

    ProcessResult result = this.context.newProcess().executable("reg").addArgs("query", path, "/v", key).run(ProcessMode.DEFAULT_CAPTURE);
    List<String> out = result.getOut();
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
