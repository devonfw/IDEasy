package com.devonfw.tools.ide.merge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jline.utils.Log;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.merge.xml.XmlMerger;
import com.devonfw.tools.ide.util.FilenameUtil;

/**
 * Implementation of {@link WorkspaceMerger} that does the whole thing:
 * <ul>
 * <li>It will recursively traverse directories.</li>
 * <li>For each setup or update file if will delegate to the according {@link FileMerger} based on the file
 * extension.</li>
 * </ul>
 */
public class DirectoryMerger extends AbstractWorkspaceMerger {

  private final Map<String, FileMerger> extension2mergerMap;

  private final FallbackMerger fallbackMerger;

  /**
   * The constructor.
   *
   * @param context the {@link #context}.
   */
  public DirectoryMerger(IdeContext context) {

    super(context);
    this.extension2mergerMap = new HashMap<>();
    PropertiesMerger propertiesMerger = new PropertiesMerger(context);
    this.extension2mergerMap.put("properties", propertiesMerger);
    this.extension2mergerMap.put("prefs", propertiesMerger); // Eclipse specific
    XmlMerger xmlMerger = new XmlMerger(context);
    this.extension2mergerMap.put("xml", xmlMerger);
    this.extension2mergerMap.put("xmi", xmlMerger);
    this.extension2mergerMap.put("launch", xmlMerger); // Eclipse specific
    JsonMerger jsonMerger = new JsonMerger(context);
    this.extension2mergerMap.put("json", jsonMerger);
    TextMerger textMerger = new TextMerger(context);
    this.extension2mergerMap.put("name", textMerger); // intellij specific
    this.extension2mergerMap.put("editorconfig", textMerger);
    this.extension2mergerMap.put("txt", textMerger);
    this.fallbackMerger = new FallbackMerger(context);
  }

  @Override
  public int merge(Path setup, Path update, EnvironmentVariables variables, Path workspace) {

    int errors = 0;
    Set<String> children = null;
    children = addChildren(setup, children);
    children = addChildren(update, children);
    if (children == null) {
      // file merge
      FileMerger merger = getMerger(workspace);
      errors += merger.merge(setup, update, variables, workspace);
    } else {
      // directory scan
      for (String filename : children) {
        errors += merge(setup.resolve(filename), update.resolve(filename), variables, workspace.resolve(filename));
      }
    }
    return errors;
  }

  private FileMerger getMerger(Path file) {

    String filename = file.getFileName().toString();
    String extension = FilenameUtil.getExtension(filename);
    if (extension == null) {
      this.context.debug("No extension for {}", file);
    } else {
      this.context.trace("Extension is {}", extension);
      FileMerger merger = this.extension2mergerMap.get(extension);
      if (merger != null) {
        return merger;
      }
    }
    return this.fallbackMerger;
  }

  @Override
  public void inverseMerge(Path workspace, EnvironmentVariables variables, boolean addNewProperties, Path update) {

    if (Files.isDirectory(update)) {
      if (!Files.isDirectory(workspace)) {
        Log.warn("Workspace is missing directory: {}", workspace);
        return;
      }
      Log.trace("Traversing directory: {}", update);
      try (Stream<Path> childStream = Files.list(update)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path updateChild = iterator.next();
          Path fileName = updateChild.getFileName();
          inverseMerge(workspace.resolve(fileName), variables, addNewProperties, update.resolve(fileName));
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list children of folder " + update, e);
      }

    } else if (Files.exists(workspace)) {
      Log.debug("Start merging of changes from workspace back to file: {}", update);
      FileMerger merger = getMerger(workspace);
      Log.trace("Using merger {}", merger.getClass().getSimpleName());
      merger.inverseMerge(workspace, variables, addNewProperties, update);
    } else {
      Log.warn("No such file or directory: {}", update);
    }
  }

  private Set<String> addChildren(Path folder, Set<String> children) {

    if (!Files.isDirectory(folder)) {
      return children;
    }
    try (Stream<Path> childStream = Files.list(folder)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        if (children == null) {
          children = new HashSet<>();
        }
        children.add(child.getFileName().toString());
      }
      return children;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list children of folder " + folder, e);
    }
  }

  @Override
  public void upgrade(Path folder) {

    try (Stream<Path> childStream = Files.list(folder)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        if (Files.isDirectory(child)) {
          upgrade(child);
        } else {
          FileMerger merger = getMerger(child);
          merger.upgrade(child);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list children of folder " + folder, e);
    }
  }

}
