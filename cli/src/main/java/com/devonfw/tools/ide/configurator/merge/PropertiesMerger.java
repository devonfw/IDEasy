package com.devonfw.tools.ide.configurator.merge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import com.devonfw.tools.ide.configurator.SortedProperties;
import com.devonfw.tools.ide.configurator.resolve.VariableResolver;
import com.devonfw.tools.ide.logging.Log;

/**
 * Implementation of {@link FileTypeMerger} for {@link Properties} files.
 *
 * @since 3.0.0
 */
public class PropertiesMerger extends FileTypeMerger {

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    SortedProperties properties = new SortedProperties();
    boolean updateFileExists = updateFile.exists();
    if (workspaceFile.exists()) {
      if (!updateFileExists) {
        Log.trace("Nothing to do as update file does not exist: " + updateFile);
        return; // nothing to do ...
      }
      load(properties, workspaceFile);
    } else if (setupFile.exists()) {
      load(properties, setupFile);
    }
    if (updateFileExists) {
      load(properties, updateFile);
    }
    resolve(properties, resolver);
    save(properties, workspaceFile);
    Log.trace("Saved merged properties to: " + workspaceFile);
  }

  /**
   * @param file the {@link File} to load.
   * @return the loaded {@link Properties}.
   */
  public static Properties load(File file) {

    Properties properties = new Properties();
    load(properties, file);
    return properties;
  }

  /**
   * @param file the {@link File} to load.
   * @return the loaded {@link Properties}.
   */
  public static Properties loadIfExists(File file) {

    Properties properties = new Properties();
    if (file != null) {
      if (file.exists()) {
        load(properties, file);
      } else {
        Log.trace("Properties file does not exist: " + file);
      }
    }
    return properties;
  }

  /**
   * @param properties the existing {@link Properties} instance.
   * @param file the properties {@link File} to load.
   */
  public static void load(Properties properties, File file) {

    Log.trace("Loading properties file " + file);
    try (InputStream in = new FileInputStream(file);
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load properties from file: " + file, e);
    }
  }

  private void resolve(Properties properties, VariableResolver resolver) {

    Set<Object> keys = properties.keySet();
    for (Object key : keys) {
      String value = properties.getProperty(key.toString());
      properties.setProperty(key.toString(), resolver.resolve(value));
    }
  }

  /**
   * @param properties the {@link Properties} to save.
   * @param file the {@link File} to save to.
   */
  public static void save(Properties properties, File file) {

    Log.trace("Saving properties file " + file);
    ensureParentDirecotryExists(file);
    try (OutputStream out = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      properties.store(writer, null);
    } catch (IOException e) {
      throw new IllegalStateException("Could not write properties to file: " + file, e);
    }
  }

  @Override
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    if (!workspaceFile.exists()) {
      Log.trace("Workspace file does not exist: " + workspaceFile.getAbsolutePath());
      return;
    }
    if (!updateFile.exists()) {
      Log.trace("Update file does not exist: " + updateFile.getAbsolutePath());
      return;
    }
    Properties updateProperties = load(updateFile);
    Properties workspaceProperties = load(workspaceFile);
    SortedProperties mergedProperties = new SortedProperties();
    mergedProperties.putAll(updateProperties);
    boolean updated = false;
    for (Object key : workspaceProperties.keySet()) {
      Object workspaceValue = workspaceProperties.get(key);
      Object updateValue = updateProperties.get(key);
      if ((updateValue != null) || addNewProperties) {
        String updateValueResolved = null;
        if (updateValue != null) {
          updateValueResolved = resolver.resolve(updateValue.toString());
        }
        if (!workspaceValue.equals(updateValueResolved)) {
          String workspaceValueInverseResolved = resolver.inverseResolve(workspaceValue.toString());
          mergedProperties.put(key, workspaceValueInverseResolved);
          updated = true;
        }
      }
    }
    if (updated) {
      save(mergedProperties, updateFile);
      Log.debug("Saved changes from " + workspaceFile.getName() + " to " + updateFile.getAbsolutePath());
    } else {
      Log.trace("No changes for " + updateFile.getAbsolutePath());
    }
  }

}
