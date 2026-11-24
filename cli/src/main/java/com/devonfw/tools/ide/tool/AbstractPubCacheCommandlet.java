package com.devonfw.tools.ide.tool;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;

/**
 * {@link AbstractLocalToolCommandlet} for tools whose dependency management writes into the shared, per-user Pub cache ({@code PUB_CACHE}, default
 * {@code ~/.pub-cache}). Left alone, that cache would be shared across all IDEasy projects and therefore violate the
 * <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox principle</a>. Both the Dart SDK and the Flutter framework (which
 * bundles Dart) rely on the Pub cache, so they share this commandlet to pin {@code PUB_CACHE} into the per-project
 * {@link IdeContext#getIdeHome() IDE_HOME} in exactly the same way; keeping the constant and the override here, in one place, guarantees the two cannot drift
 * apart.
 */
public abstract class AbstractPubCacheCommandlet extends AbstractLocalToolCommandlet {

  /** The name of the environment variable pointing to the Pub cache. */
  public static final String ENV_PUB_CACHE = "PUB_CACHE";

  /** The folder name for the per-project Pub cache inside {@link IdeContext#getIdeHome() IDE_HOME}. */
  public static final String PUB_CACHE_FOLDER = ".pub-cache";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  protected AbstractPubCacheCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  /**
   * Pins the Pub cache into the per-project {@link IdeContext#getIdeHome() IDE_HOME} so that the tool's dependencies (stored in {@code PUB_CACHE}) are not
   * shared across projects (see the <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox principle</a>).
   *
   * @param environmentContext the {@link EnvironmentContext} where to {@link EnvironmentContext#withEnvVar(String, String) set environment variables}.
   * @param toolInstallation the {@link ToolInstallation}.
   * @param additionalInstallation {@code true} if the {@link ToolInstallation} is an additional installation, {@code false} otherwise.
   */
  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // The tool resolves its dependencies into the shared user home (PUB_CACHE, default ~/.pub-cache) which would leak across projects; pin it per project.
    Path ideHome = this.context.getIdeHome();
    if (ideHome != null) {
      environmentContext.withEnvVar(ENV_PUB_CACHE, ideHome.resolve(PUB_CACHE_FOLDER).toString());
    }
  }
}
