package com.devonfw.tools.ide.tool.java;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link ToolCommandlet} for Java (Java Virtual Machine and Java Development Kit).
 */
public class Java extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Java(IdeContext context) {

    super(context, "java", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
