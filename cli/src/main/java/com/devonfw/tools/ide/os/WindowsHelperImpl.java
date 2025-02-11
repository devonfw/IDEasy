package com.devonfw.tools.ide.os;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
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
  public String getUserEnvironmentValue(String key) {

    return getRegistryValue(HKCU_ENVIRONMENT, key);
  }

  @Override
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
