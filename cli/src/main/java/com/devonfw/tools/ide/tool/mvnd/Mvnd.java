package com.devonfw.tools.ide.tool.mvnd;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.completion.AutoCompletionRegistry;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/tools/mvnd.html/">maven daemon</a>.
 */
public class Mvnd extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvnd(IdeContext context) {
    super(context, "mvnd", Set.of(Tag.JAVA, Tag.BUILD));
  }

  /**
   * Initializes Maven Daemon-specific auto-completion candidates.
   *
   * @param registry the {@link AutoCompletionRegistry} to initialize.
   */
  @Override
  protected void initAutoCompletionRegistry(AutoCompletionRegistry registry) {

    // lifecycle phases
    registry.add("clean");
    registry.add("validate");
    registry.add("compile");
    registry.add("test");
    registry.add("package");
    registry.add("verify");
    registry.add("install");
    registry.add("deploy");

// plugin goals
    registry.add("dependency:tree");
    registry.add("dependency:list");
    registry.add("help:effective-settings");
    registry.add("exec:java");

// common maven options
    registry.add("-DskipTests");
    registry.add("-Dmaven.test.skip=true");
    registry.add("-P");
    registry.add("-pl");
    registry.add("-am");
    registry.add("-amd");
    registry.add("--also-make");
    registry.add("--also-make-dependents");
    registry.add("--fail-at-end");
    registry.add("--fail-fast");
    registry.add("-T1C");
    registry.add("-q");
    registry.add("-X");
    registry.add("-e");
    registry.add("-U");
    registry.add("-o");
    registry.add("-f");
    registry.add("-s");
    registry.add("-rf");

// exec plugin properties
    registry.add("-Dexec.mainClass=");
    registry.add("-Dexec.args=");
    registry.add("-DdeployAtEnd=true");

// mvnd-specific commands
    registry.add("--status");
    registry.add("--stop");
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }
}
