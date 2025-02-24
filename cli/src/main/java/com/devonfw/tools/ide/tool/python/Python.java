package com.devonfw.tools.ide.tool.python;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Python(IdeContext context) {

    super(context, "python", Set.of(Tag.PYTHON));
  }

  @Override
  public void printHelp(NlsBundle bundle) {

    this.context.info("For a list of supported options and arguments, use \"python --help\"");
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }
}
