package com.devonfw.tools.ide.tool.gcloganalyzer;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for GC Log Analyzer by Azul Systems.
 */
public class GcLogAnalyzer extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GcLogAnalyzer(IdeContext context) {

    super(context, "gcloganalyzer", Set.of(Tag.JAVA, Tag.ANALYSE));
  }

  @Override
  protected void doRun() {

    runTool(ProcessMode.BACKGROUND, null, this.arguments.asList());
  }

  @Override
  public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

    pc.directory(getToolPath());
    pc.executable("java");
    pc.addArg("-jar");
    pc.addArg(resolveGcLogAnalyzerJarPath().getFileName().toString());
    pc.addArgs(args);
    return pc.run(processMode);
  }

  private Path resolveGcLogAnalyzerJarPath() {

    String version = getInstalledVersion().toString();
    if (version.endsWith(".0.0")) {
      version = version.substring(0, version.length() - 2);
    }
    return getToolPath().resolve("GCLogAnalyzer-" + version + "-ca.jar");
  }
}
