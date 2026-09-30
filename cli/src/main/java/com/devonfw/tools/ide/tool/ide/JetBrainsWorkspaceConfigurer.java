package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Configures JetBrains IDE workspaces by splitting the tool-specific workspace-template merge into targeted merges (see #2531): the {@code .idea}
 * setup/update subtree is merged into the {@code .idea} folder of the workspace, and the {@code .«ide»/config} setup/update subtree is merged into the
 * out-of-workspace metadata config folder ({@link #getIdeMetadataConfigPath()}). The tool-agnostic generic templates are still merged wholesale by the
 * base class. The top-level {@code idea.properties} in the JetBrains settings template is never merged, since the targeted merges only descend into
 * {@code .idea} and {@code .«ide»/config}.
 */
public class JetBrainsWorkspaceConfigurer extends IdeWorkspaceConfigurer {

  private static final Logger LOG = LoggerFactory.getLogger(JetBrainsWorkspaceConfigurer.class);

  /** The name of the out-of-workspace config folder, kept in line with VSCode's {@code $IDE_HOME/.ide/«ide»/«workspace»/config}. */
  private static final String CONFIG_FOLDER = "config";

  /** The IDE project metadata folder name (e.g. {@code .idea}), merged into the workspace as usual. */
  private static final String IDEA_FOLDER = ".idea";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param toolName the name of the JetBrains IDE tool (e.g. "intellij", "android-studio", "pycharm").
   */
  public JetBrainsWorkspaceConfigurer(IdeContext context, String toolName) {

    super(context, toolName);
  }

  /**
   * @return the {@link Path} to the out-of-workspace config folder at {@code $IDE_HOME/.ide/«ide»/«workspace»/config} used for {@code idea.config.path}.
   */
  public Path getIdeMetadataConfigPath() {

    return this.context.getIdeHome().resolve(IdeContext.FOLDER_DOT_IDE).resolve(this.toolName).resolve(this.context.getWorkspaceName())
        .resolve(CONFIG_FOLDER);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Splits the tool-specific template folder into two targeted merges (see class javadoc) instead of merging it wholesale.
   */
  @Override
  protected int mergeToolWorkspace(Path templatesFolder, Path workspaceFolder, Map<Path, Path> redirects, Set<Path> excludes, int errors) {

    Path setup = templatesFolder.resolve(IdeContext.FOLDER_SETUP);
    Path update = templatesFolder.resolve(IdeContext.FOLDER_UPDATE);
    if (!Files.isDirectory(setup) && !Files.isDirectory(update)) {
      return errors;
    }
    LOG.debug("Merging JetBrains tool-specific workspace templates from {}...", templatesFolder);
    // pass 1: the .idea setup/update subtree -> the .idea folder of the workspace
    errors = mergeSubtree(setup, update, IDEA_FOLDER, workspaceFolder.resolve(IDEA_FOLDER), errors);
    // pass 2: the .«ide»/config setup/update subtree -> the out-of-workspace metadata config folder
    errors = mergeSubtree(setup, update, "." + this.toolName + "/" + CONFIG_FOLDER, getIdeMetadataConfigPath(), errors);
    return errors;
  }

  /**
   * Merges a single template subtree (e.g. {@code .idea} or {@code .intellij/config}) from the given setup/update templates into the given target. If
   * neither the setup nor the update subtree exists, this is a no-op.
   *
   * @param setup the setup folder of the tool-specific template folder.
   * @param update the update folder of the tool-specific template folder.
   * @param subtree the workspace-relative path of the subtree to merge (e.g. {@code .idea} or {@code .intellij/config}).
   * @param target the {@link Path} to merge the subtree into.
   * @param errors the running error count.
   * @return the updated error count.
   */
  private int mergeSubtree(Path setup, Path update, String subtree, Path target, int errors) {

    Path setupSub = setup.resolve(subtree);
    Path updateSub = update.resolve(subtree);
    if (!Files.isDirectory(setupSub) && !Files.isDirectory(updateSub)) {
      LOG.trace("Skipping non-existing JetBrains workspace template subtree {}.", subtree);
      return errors;
    }
    return errors + this.context.getWorkspaceMerger().merge(setupSub, updateSub, this.context.getVariables(), target, Map.of());
  }

  /**
   * {@inheritDoc}
   * <p>
   * Resolves the extra-SDK template (declared under the tool metadata folder, e.g. {@code .intellij/config/options/jdk.table.xml}) to the
   * out-of-workspace metadata folder (not the workspace), so the import lands in {@code $IDE_HOME/.ide/«ide»/«workspace»/config} (see #2531).
   */
  @Override
  protected Path resolveExtraSdkTarget(Path templatePath, Map<Path, Path> redirects, Set<Path> excludes) {

    // the template is declared relative to the workspace under the tool metadata folder (e.g. .intellij/...); that folder is now out-of-workspace at
    // $IDE_HOME/.ide/«ide»/«workspace», so strip the leading ".«ide»" segment and resolve there
    return getIdeMetadataConfigPath().getParent().resolve(templatePath.subpath(1, templatePath.getNameCount()));
  }
}
