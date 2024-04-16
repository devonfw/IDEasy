package com.devonfw.tools.ide.commandlet;

import java.util.Collection;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * {@link Commandlet} to print the environment variables.
 */
public final class EnvironmentCommandlet extends Commandlet {

  /** {@link FlagProperty} to enable Bash (MSys) path conversion on Windows. */
  public final FlagProperty bash;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EnvironmentCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.bash = add(new FlagProperty("--bash", false, null));
  }

  @Override
  public String getName() {

    return "env";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return true;
  }

  @Override
  public boolean isSuppressStepSuccess() {

    return true;
  }

  @Override
  public void run() {

    Collection<VariableLine> variables = this.context.getVariables().collectVariables();
    for (VariableLine line : variables) {
      if (this.context.getSystemInfo().isWindows()) {
        line = normalizeWindowsValue(line);
      }
      String lineValue = line.getValue();
      if (IdeVariables.PATH.getName().equals(line.getName())) {
        lineValue = this.context.getPath().toString(this.bash.isTrue());
      }
      lineValue = "\"" + lineValue + "\"";
      line = line.withValue(lineValue);
      this.context.info(line.toString());
    }
  }

  VariableLine normalizeWindowsValue(VariableLine line) {

    String value = line.getValue();
    String normalized = normalizeWindowsValue(value);
    if (normalized != value) {
      line = line.withValue(normalized);
    }
    return line;
  }

  String normalizeWindowsValue(String value) {

    WindowsPathSyntax pathSyntax;
    if (this.bash.isTrue()) {
      pathSyntax = WindowsPathSyntax.MSYS;
    } else {
      pathSyntax = WindowsPathSyntax.WINDOWS;
    }
    String drive = WindowsPathSyntax.WINDOWS.getDrive(value);
    if (drive == null) {
      drive = WindowsPathSyntax.MSYS.getDrive(value);
    }
    if (drive != null) {
      value = pathSyntax.replaceDrive(value, drive);
    }
    return value;
  }
}
