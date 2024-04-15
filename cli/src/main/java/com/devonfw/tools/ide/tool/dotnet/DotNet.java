package com.devonfw.tools.ide.tool.dotnet;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
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

  @Override
  public void run() {

    AbstractIdeContext abstractIdeContext = (AbstractIdeContext) context;
    abstractIdeContext.setDefaultExecutionDirectory(context.getIdeHome());

    String[] args = this.arguments.asArray();

    if (context.isQuietMode()) {
      runTool(ProcessMode.DEFAULT_SILENT, null, args);
    } else {
      runTool(ProcessMode.DEFAULT, null, args);
    }
  }

}