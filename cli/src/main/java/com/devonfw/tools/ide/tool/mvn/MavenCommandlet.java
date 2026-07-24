package com.devonfw.tools.ide.tool.mvn;


import java.util.Set;


import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.completion.AutoCompletionRegistry;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;


/**
 * Abstract base class for Maven-compatible tool commandlets.
 */
public abstract class MavenCommandlet extends LocalToolCommandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the name of the tool.
   */
  protected MavenCommandlet(IdeContext context, String tool) {

    super(context, tool, Set.of(Tag.JAVA, Tag.BUILD));
  }

  /**
   * Initializes common Maven auto-completion candidates.
   *
   * @param registry the {@link AutoCompletionRegistry} to initialize.
   */
  @Override
  protected void initAutoCompletionRegistry(AutoCompletionRegistry registry) {

    registry.add("clean");
    registry.add("package");
    registry.add("install");
    registry.add("deploy");
    registry.add("test");
    registry.add("verify");
    registry.add("validate");
    registry.add("compile");
    registry.add("dependency:tree");
    registry.add("dependency:list");
    registry.add("help:effective-settings");
    registry.add("-DskipTests");
    registry.add("-Dmaven.test.skip=true");
    registry.add("exec:java");
    registry.add("-Dexec.mainClass=");
    registry.add("-Dexec.args=");
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
    registry.add("-s", "--settings");
    registry.add("-rf");
    registry.add("-DdeployAtEnd=true");
    registry.add("-Dmaven.multiModuleProjectDirectory=");
    registry.add("-Dmaven.repo.local=");
    registry.add("-T", "--threads");
    registry.add("-Dstyle.color=");
    registry.add("-Duser.dir=");
    registry.add("-Duser.home=");
  }
}


