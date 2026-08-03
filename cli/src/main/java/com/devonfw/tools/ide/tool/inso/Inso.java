package com.devonfw.tools.ide.tool.inso;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import java.util.Set;


/**
 * {@link ToolCommandlet} for <a href="https://github.com/Kong/Insomnia">Inso CLI</a>.
 */
public class Inso extends LocalToolCommandlet{

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */

  public Inso(IdeContext context) {

    super(context, "inso", Set.of(Tag.REST, Tag.HTTP));

  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

}
