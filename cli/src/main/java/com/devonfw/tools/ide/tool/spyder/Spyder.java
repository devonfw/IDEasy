package com.devonfw.tools.ide.tool.spyder;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://www.spyder-ide.org/">Spyder</a> installed via PyPI (pip).
 * <p>
 * Spyder is a Python package and therefore must be managed via the Python/pip tooling.
 */
public class Spyder extends PipBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spyder(IdeContext context) {

    super(context, "spyder", Set.of(Tag.SPYDER, Tag.PYTHON));
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }

  @Override
  protected String getBinaryName() {

    return "spyder";
  }

}
