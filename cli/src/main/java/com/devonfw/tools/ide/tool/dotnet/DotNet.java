package com.devonfw.tools.ide.tool.dotnet;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

import java.util.Set;

public class DotNet extends LocalToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}. method.
   */
  public DotNet(IdeContext context) {

    super(context, "dotnet", Set.of(Tag.DOTNET, Tag.CS));
  }
}