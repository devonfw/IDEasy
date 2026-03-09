package com.devonfw.tools.ide.merge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.SortedProperties;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Implementation of {@link FileMerger} for {@link Properties} files.
 */
public class PropertiesMerger extends FileMerger {

  private static final Logger LOG = LoggerFactory.getLogger(PropertiesMerger.class);

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public PropertiesMerger(IdeContext context) {

    super(context);
  }

  @Override
  protected void doMerge(Path setup, Path update, EnvironmentVariables resolver, Path workspace) {

    FileAccess fileAccess = this.context.getFileAccess();
    SortedProperties properties = new SortedProperties();
    boolean updateFileExists = Files.exists(update);
    Path template = setup;
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        LOG.trace("Nothing to do as update file does not exist: {}", update);
        return; // nothing to do ...
      }
      fileAccess.readProperties(workspace, properties);
    } else if (Files.exists(setup)) {
      fileAccess.readProperties(setup, properties);
    }
    if (updateFileExists) {
      fileAccess.readProperties(update, properties);
      template = update;
    }
    resolve(properties, resolver, template.toString());
    fileAccess.writeProperties(properties, workspace, true);
    LOG.trace("Saved merged properties to: {}", workspace);
  }

  private void resolve(Properties properties, EnvironmentVariables variables, Object src) {

    Set<Object> keys = properties.keySet();
    for (Object key : keys) {
      String value = properties.getProperty(key.toString());
      properties.setProperty(key.toString(), variables.resolve(value, src, this.legacySupport));
    }
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace)) {
      LOG.trace("Workspace file does not exist: {}", workspace);
      return;
    }
    if (!Files.exists(update)) {
      LOG.trace("Update file does not exist: {}", update);
      return;
    }
    Object src = workspace.getFileName();
    FileAccess fileAccess = this.context.getFileAccess();
    Properties updateProperties = fileAccess.readProperties(update);
    Properties workspaceProperties = fileAccess.readProperties(workspace);
    SortedProperties mergedProperties = new SortedProperties();
    mergedProperties.putAll(updateProperties);
    boolean updated = false;
    for (Object key : workspaceProperties.keySet()) {
      Object workspaceValue = workspaceProperties.get(key);
      Object updateValue = updateProperties.get(key);
      if ((updateValue != null) || addNewProperties) {
        String updateValueResolved = null;
        if (updateValue != null) {
          updateValueResolved = variables.resolve(updateValue.toString(), src, this.legacySupport);
        }
        if (!workspaceValue.equals(updateValueResolved)) {
          String workspaceValueInverseResolved = variables.inverseResolve(workspaceValue.toString(), src);
          mergedProperties.put(key, workspaceValueInverseResolved);
          updated = true;
        }
      }
    }
    if (updated) {
      fileAccess.writeProperties(mergedProperties, update);
      LOG.debug("Saved changes from: {} to: {}", workspace.getFileName(), update);
    } else {
      LOG.trace("No changes for: {}", update);
    }
  }

  @Override
  protected boolean doUpgrade(Path workspaceFile) throws Exception {

    return doUpgradeTextContent(workspaceFile);
  }
}
