package com.devonfw.tools.ide.tool.dotnet;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link LocalToolCommandlet} for <a href="https://docs.microsoft.com/en-us/dotnet/core/tools/">dotnet</a>. The .NET CLI (Command Line Interface)
 * cross-platform tool for building, running, and managing .NET applications.
 */
public class DotNet extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}. method.
   */
  public DotNet(IdeContext context) {

    super(context, "dotnet", Set.of(Tag.DOTNET, Tag.CS));
  }

  @Override
  public String getToolHelpArguments() {

    return "help";
  }
}
