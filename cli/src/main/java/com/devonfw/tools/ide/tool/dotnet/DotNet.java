package com.devonfw.tools.ide.tool.dotnet;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

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

    if (args != null && args.length == 1) {
      String firstArgument = args[0];
      if (firstArgument.equals("create") || firstArgument.equals("c")) {
        List<String> newArgs = List.of("new", "Devon4NetAPI");
        args = newArgs.toArray(String[]::new);

      }
    }

    if (context.isQuietMode()) {
      runTool(null, true, args);
    } else {
      runTool(false, null, args);
    }
  }

  @Override
  public void postInstall() {

    super.postInstall();
    if (devon4NetTemplateExists()) {
      this.context.debug("Devon4net template already installed.");
    } else {
      installDevon4NetTemplate();
    }

  }

  private boolean devon4NetTemplateExists() {

    FileAccess fileAccess = this.context.getFileAccess();
    Path nugetPackagePath = this.context.getUserHome().resolve(".templateengine/packages");

    Path webApiTemplatePath = fileAccess.findFirst(nugetPackagePath,
        p -> p.getFileName().toString().startsWith("Devon4Net.WebAPI.Template"), false);

    return webApiTemplatePath != null;
  }

  // dotnet new uninstall 'Devon4Net.WebAPI'
  private void installDevon4NetTemplate() {

    String[] args = { "new", "install", "devon4net.WebApi.Template" };
    // run dotnet
    Path toolPath = getToolPath();
    Path binaryPath = toolPath.resolve(getBinaryName());
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(binaryPath)
        .addArgs(args);

    pc.run(false, false);

  }

}
