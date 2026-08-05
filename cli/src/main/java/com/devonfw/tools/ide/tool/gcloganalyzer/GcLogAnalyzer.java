package com.devonfw.tools.ide.tool.gcloganalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
  public ProcessResult runTool(List<String> args) {

    return runTool(ProcessMode.BACKGROUND, null, args);
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

    List<Path> jarFiles = this.context.getFileAccess().listChildren(
        getToolPath(),
        path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")
    );

    if (jarFiles.size() == 1) {
      return jarFiles.getFirst();
    }

    return jarFiles.stream().filter(path -> path.getFileName().toString()
            .regionMatches(true, 0, "GCLogAnalyzer-", 0, "GCLogAnalyzer-".length())).findFirst()
        .orElseThrow(() -> new IllegalStateException("Could not find GC Log Analyzer JAR in " + getToolPath()));
  }
}
