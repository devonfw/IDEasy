package com.devonfw.tools.ide.tool.spyder;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.pip.Pip;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

/**
 * {@link PipBasedCommandlet} for <a href="https://www.spyder-ide.org/">Spyder</a>.
 */
public class Spyder extends PipBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spyder(IdeContext context) {
    super(context, "spyder", Set.of(Tag.SPYDER));
  }


}
