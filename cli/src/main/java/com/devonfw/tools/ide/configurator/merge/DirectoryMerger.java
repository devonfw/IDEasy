package com.devonfw.tools.ide.configurator.merge;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.configurator.resolve.VariableResolver;
import com.devonfw.tools.ide.logging.Log;

/**
 * Implementation of {@link FileMerger} that does the whole thing:
 * <ul>
 * <li>It will recursively traverse directories.</li>
 * <li>For each setup or update file if will delegate to the according {@link FileTypeMerger} based on the file
 * extension.</li>
 * </ul>
 *
 * @since 3.0.0
 */
public class DirectoryMerger implements FileMerger {

  private final Map<String, FileTypeMerger> extension2mergerMap;

  private final FallbackMerger fallbackMerger;

  /**
   * The constructor.
   */
  public DirectoryMerger() {

    super();
    this.extension2mergerMap = new HashMap<>();
    PropertiesMerger propertiesMerger = new PropertiesMerger();
    this.extension2mergerMap.put(".properties", propertiesMerger);
    this.extension2mergerMap.put(".prefs", propertiesMerger); // Eclipse specific
    XmlMerger xmlMerger = new XmlMerger();
    this.extension2mergerMap.put(".xml", xmlMerger);
    this.extension2mergerMap.put(".xmi", xmlMerger);
    this.extension2mergerMap.put(".launch", xmlMerger); // Eclipse specific
    JsonMerger jsonMerger = new JsonMerger();
    this.extension2mergerMap.put(".json", jsonMerger);
    this.fallbackMerger = new FallbackMerger();
  }

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    String[] children = null;
    if (setupFile.isDirectory()) {
      children = setupFile.list();
    }
    if (updateFile.isDirectory()) {
      if (children == null) {
        children = updateFile.list();
      } else {
        Set<String> set = new HashSet<>();
        addChildren(children, set);
        addChildren(updateFile.list(), set);
        children = set.toArray(children);
      }
    }
    if (children == null) {
      // file merge
      FileTypeMerger merger = getMerger(workspaceFile);
      merger.merge(setupFile, updateFile, resolver, workspaceFile);
    } else {
      // directory scan
      for (String filename : children) {
        merge(new File(setupFile, filename), new File(updateFile, filename), resolver,
            new File(workspaceFile, filename));
      }
    }
  }

  private FileTypeMerger getMerger(File file) {

    String filename = file.getName();
    int lastDot = filename.lastIndexOf('.');
    if (lastDot > 0) {
      String extension = filename.substring(lastDot);
      Log.trace("Extension is " + extension);
      FileTypeMerger merger = this.extension2mergerMap.get(extension);
      if (merger != null) {
        return merger;
      }
    } else {
      Log.debug("No extension for " + file);
    }
    return this.fallbackMerger;
  }

  @Override
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    if (updateFile.isDirectory()) {
      if (!workspaceFile.isDirectory()) {
        Log.warn("Workspace is missing directory: " + workspaceFile);
        return;
      }
      Log.trace("Traversing directory " + updateFile);
      for (File child : updateFile.listFiles()) {
        inverseMerge(new File(workspaceFile, child.getName()), resolver, addNewProperties,
            new File(updateFile, child.getName()));
      }
    } else if (workspaceFile.exists()) {
      Log.debug("Start merging of changes from workspace back to file " + updateFile);
      FileTypeMerger merger = getMerger(workspaceFile);
      Log.trace("Using merger " + merger.getClass().getSimpleName());
      merger.inverseMerge(workspaceFile, resolver, addNewProperties, updateFile);
    } else {
      Log.warn("No such file or directory: " + updateFile);
    }
  }

  private void addChildren(String[] filenames, Set<String> children) {

    for (String filename : filenames) {
      children.add(filename);
    }
  }

}
