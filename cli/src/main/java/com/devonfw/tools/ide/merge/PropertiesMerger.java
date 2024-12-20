package com.devonfw.tools.ide.merge;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.SortedProperties;

/**
 * Implementation of {@link FileMerger} for {@link Properties} files.
 */
public class PropertiesMerger extends FileMerger {

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

    SortedProperties properties = new SortedProperties();
    boolean updateFileExists = Files.exists(update);
    Path template = setup;
    if (Files.exists(workspace)) {
      if (!updateFileExists) {
        this.context.trace("Nothing to do as update file does not exist: {}", update);
        return; // nothing to do ...
      }
      load(properties, workspace);
    } else if (Files.exists(setup)) {
      load(properties, setup);
    }
    if (updateFileExists) {
      load(properties, update);
      template = update;
    }
    resolve(properties, resolver, template.toString());
    save(properties, workspace);
    this.context.trace("Saved merged properties to: {}", workspace);
  }

  /**
   * @param file the {@link Path} to load.
   * @return the loaded {@link Properties}.
   */
  public Properties load(Path file) {

    Properties properties = new Properties();
    load(properties, file);
    return properties;
  }

  /**
   * @param file the {@link Path} to load.
   * @return the loaded {@link Properties}.
   */
  public Properties loadIfExists(Path file) {

    Properties properties = new Properties();
    if (file != null) {
      if (Files.exists(file)) {
        load(properties, file);
      } else {
        this.context.trace("Properties file does not exist: {}", file);
      }
    }
    return properties;
  }

  /**
   * @param properties the existing {@link Properties} instance.
   * @param file the properties {@link Path} to load.
   */
  public void load(Properties properties, Path file) {

    this.context.trace("Loading properties file: {}", file);
    try (Reader reader = Files.newBufferedReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load properties from file: " + file, e);
    }
  }

  private void resolve(Properties properties, EnvironmentVariables variables, Object src) {

    Set<Object> keys = properties.keySet();
    for (Object key : keys) {
      String value = properties.getProperty(key.toString());
      properties.setProperty(key.toString(), variables.resolve(value, src, this.legacySupport));
    }
  }

  /**
   * @param properties the {@link Properties} to save.
   * @param file the {@link Path} to save to.
   */
  public void save(Properties properties, Path file) {

    this.context.trace("Saving properties file: {}", file);
    ensureParentDirectoryExists(file);
    try (Writer writer = Files.newBufferedWriter(file)) {
      properties.store(writer, null);
    } catch (IOException e) {
      throw new IllegalStateException("Could not write properties to file: " + file, e);
    }
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (!Files.exists(workspace)) {
      this.context.trace("Workspace file does not exist: {}", workspace);
      return;
    }
    if (!Files.exists(update)) {
      this.context.trace("Update file does not exist: {}", update);
      return;
    }
    Object src = workspace.getFileName();
    Properties updateProperties = load(update);
    Properties workspaceProperties = load(workspace);
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
      save(mergedProperties, update);
      this.context.debug("Saved changes from: {} to: {}", workspace.getFileName(), update);
    } else {
      this.context.trace("No changes for: {}", update);
    }
  }

  @Override
  protected boolean doUpgrade(Path workspaceFile) throws Exception {

    return doUpgradeTextContent(workspaceFile);
  }
}
