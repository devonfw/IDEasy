package com.devonfw.tools.ide.tool.pandoc;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;

/**
 * {@link AbstractToolCommandlet} for <a href="https://pandoc.org/">Pandoc</a>, a universal document converter.
 */
public class Pandoc extends AbstractLocalToolCommandlet {

  private static final String DATA_DIR_OPTION = "--data-dir";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pandoc(IdeContext context) {

    super(context, "pandoc", Set.of(Tag.DOCUMENTATION));
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    Path confPath = this.context.getConfPath();
    // Pandoc selects these modes only when the subcommand is the first argument.
    boolean subcommand = !args.isEmpty() && ("lua".equals(args.getFirst()) || "server".equals(args.getFirst()));
    if (confPath != null && !subcommand && args.stream().takeWhile(arg -> !arg.equals("--"))
        .noneMatch(arg -> arg.equals(DATA_DIR_OPTION) || arg.startsWith(DATA_DIR_OPTION + "="))) {
      pc.addArg(DATA_DIR_OPTION + "=" + confPath.resolve(getName()));
    }
    super.configureToolArgs(pc, processMode, args);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
