package com.devonfw.tools.ide.tool.oc;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://docs.openshift.com/">Openshift CLI</a>.
 */
public class Oc extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}
   */
  public Oc(IdeContext context) {

    super(context, "oc", Set.of(Tag.CLOUD));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
