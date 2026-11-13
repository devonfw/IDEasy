package com.devonfw.tools.ide.tool.flutter;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link AbstractLocalToolCommandlet} for the Flutter SDK (including the bundled Dart SDK).
 * <p>
 * Flutter resolves its dependencies into the Pub cache, which lives in the shared user home ({@code ~/.pub-cache}) by default and would therefore be used
 * across all IDEasy projects (a side-effect that violates the <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox
 * principle</a>). This commandlet pins {@code PUB_CACHE} into the per-project {@link IdeContext#getIdeHome() IDE_HOME} (see {@link #PUB_CACHE_FOLDER}) so
 * that each project keeps its own dependency cache, exactly like
 * {@link com.devonfw.tools.ide.tool.npm.Npm npm} does with {@code npm_config_prefix}.
 */
public class Flutter extends AbstractLocalToolCommandlet {

  /** The folder name for the per-project Pub cache inside {@link IdeContext#getIdeHome() IDE_HOME}. */
  public static final String PUB_CACHE_FOLDER = ".pub-cache";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Flutter(IdeContext context) {

    super(context, "flutter", Set.of(Tag.FLUTTER));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  /**
   * Pins the Pub cache into the per-project {@link IdeContext#getIdeHome() IDE_HOME} so that Flutter's dependencies are not shared across projects (see the
   * <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox principle</a>).
   *
   * @param environmentContext the {@link EnvironmentContext} where to {@link EnvironmentContext#withEnvVar(String, String) set environment variables}.
   * @param toolInstallation the {@link ToolInstallation}.
   * @param additionalInstallation {@code true} if the {@link ToolInstallation} is an additional installation, {@code false} otherwise.
   */
  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // Flutter resolves its dependencies into the shared user home (PUB_CACHE, default ~/.pub-cache) which would leak across projects; pin it per project.
    Path ideHome = this.context.getIdeHome();
    if (ideHome != null) {
      environmentContext.withEnvVar("PUB_CACHE", ideHome.resolve(PUB_CACHE_FOLDER).toString());
    }
  }
}
