package com.devonfw.tools.ide.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.variable.VariableDefinition;
import com.devonfw.tools.ide.variable.VariableSyntax;

/**
 * {@link WorkspaceMerger} responsible for a single type of file.
 */
public abstract class FileMerger extends AbstractWorkspaceMerger {

  protected final boolean legacySupport;

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public FileMerger(IdeContext context) {

    super(context);
    this.legacySupport = Boolean.TRUE.equals(IdeVariables.IDE_VARIABLE_SYNTAX_LEGACY_SUPPORT_ENABLED.get(context));
  }

  /**
   * @param sourceFile Path to source file.
   * @param targetFile Path to target file.
   */
  protected void copy(Path sourceFile, Path targetFile) {

    ensureParentDirectoryExists(targetFile);
    try {
      Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy file " + sourceFile + " to " + targetFile, e);
    }
  }

  @Override
  public final int merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {
    try {
      doMerge(setup, update, variables, workspace);
    } catch (Exception e) {
      this.context.warning("Failed to merge workspace file {} with update template {} and setup file {}!", workspace, update, setup);
    }
    return 0;
  }

  /**
   * Same as {@link #merge(Path, Path, EnvironmentVariables, Path)} but without error handling.
   *
   * @param setup the setup {@link Path} for creation.
   * @param update the update {@link Path} for creation and update.
   * @param variables the {@link EnvironmentVariables} to {@link EnvironmentVariables#resolve(String, Object) resolve variables}.
   * @param workspace the workspace {@link Path} to create or update.
   */
  protected abstract void doMerge(Path setup, Path update, EnvironmentVariables variables, Path workspace);

  @Override
  public void upgrade(Path workspaceFile) {

    try {
      boolean modified = doUpgrade(workspaceFile);
      if (modified) {
        this.context.debug("Successfully migrated file {}", workspaceFile);
      } else {
        this.context.trace("Nothing to migrate in file {}", workspaceFile);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to update file " + workspaceFile, e);
    }
  }

  /**
   * @param workspaceFile the {@link Path} to the file to migrate.
   * @return {@code true} if the file was migrated (modified), {@code false} otherwise.
   * @throws Exception on error.
   * @see #upgrade(Path)
   */
  protected abstract boolean doUpgrade(Path workspaceFile) throws Exception;

  /**
   * Implementation for {@link #doUpgrade(Path)} in case of simple text file format.
   *
   * @param workspaceFile the {@link Path} to the file to migrate.
   * @return {@code true} if the file was migrated (modified), {@code false} otherwise.
   * @throws IOException on error.
   */
  protected boolean doUpgradeTextContent(Path workspaceFile) throws IOException {

    String content = Files.readString(workspaceFile);
    String migratedContent = upgradeWorkspaceContent(content);
    boolean modified = !migratedContent.equals(content);
    if (modified) {
      Files.writeString(workspaceFile, migratedContent);
    }
    return modified;
  }

  /**
   * @param content the content from a workspace template file that may contain legacy stuff like old variable syntax.
   * @return the given {@link String} with potential legacy constructs being resolved.
   */
  protected String upgradeWorkspaceContent(String content) {

    VariableSyntax syntax = VariableSyntax.CURLY;
    Matcher matcher = syntax.getPattern().matcher(content);
    StringBuilder sb = null;
    while (matcher.find()) {
      if (sb == null) {
        sb = new StringBuilder(content.length() + 8);
      }
      String variableName = syntax.getVariable(matcher);
      String replacement;
      VariableDefinition<?> variableDefinition = IdeVariables.get(variableName);
      if (variableDefinition != null) {
        variableName = variableDefinition.getName(); // ensure legacy name gets replaced with new name
        replacement = VariableSyntax.SQUARE.create(variableName);
      } else if (variableName.equals("SETTINGS_PATH")) {
        replacement = "$[IDE_HOME]/settings";
      } else {
        replacement = matcher.group(); // leave ${variableName} untouched
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    if (sb == null) {
      return content;
    }
    matcher.appendTail(sb);
    return sb.toString().replace("\\conf\\.m2\\", "/conf/.m2/").replace("/conf/.m2/", "/conf/mvn/");
  }


}
