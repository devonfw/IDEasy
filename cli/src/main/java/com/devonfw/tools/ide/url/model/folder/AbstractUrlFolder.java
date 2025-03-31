package com.devonfw.tools.ide.url.model.folder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.url.model.AbstractUrlArtifact;
import com.devonfw.tools.ide.url.model.UrlArtifactWithParent;

/**
 * Class from which UrlRepository inherits, as its objects don't have a parent, but possibly child objects of the class UrlTool.
 *
 * @param <C> Type of the child object.
 */
public abstract class AbstractUrlFolder<C extends UrlArtifactWithParent<?>> extends AbstractUrlArtifact
    implements UrlFolder<C> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractUrlFolder.class);

  private final Map<String, C> childMap;

  private final Set<String> childNames;

  private final Collection<C> children;

  /**
   * The constructor.
   *
   * @param path the {@link #getPath() path}.
   * @param name the {@link #getName() filename}.
   */
  public AbstractUrlFolder(Path path, String name) {

    super(path, name);
    this.childMap = new HashMap<>();
    this.childNames = Collections.unmodifiableSet(this.childMap.keySet());
    this.children = Collections.unmodifiableCollection(this.childMap.values());
  }

  @Override
  public int getChildCount() {

    load(false);
    return this.childMap.size();
  }

  @Override
  public C getChild(String name) {

    load(false);
    if ("*".equals(name)) {
      return this.childMap.get("latest");
    }
    return this.childMap.get(name);
  }

  @Override
  public C getOrCreateChild(String name) {

    return this.childMap.computeIfAbsent(name, p -> newChild(name));
  }

  @Override
  public void deleteChild(String name) {

    C child = this.childMap.remove(name);
    if (child != null) {
      delete(child.getPath());
    }
  }

  private static void delete(Path path) {

    LOG.debug("Deleting {}", path);
    if (Files.exists(path)) {
      try {
        deleteRecursive(path);
      } catch (IOException e) {
        throw new RuntimeException("Failed to delete " + path);
      }
    } else {
      LOG.warn("Could not delete file {} because it does not exist.", path);
    }
  }

  private static void deleteRecursive(Path path) throws IOException {

    if (Files.isDirectory(path)) {
      try (Stream<Path> childStream = Files.list(path)) {
        Iterator<Path> iterator = childStream.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          deleteRecursive(child);
        }
      }
    }
    LOG.trace("Deleting {}", path);
    Files.delete(path);

  }

  @Override
  public Collection<C> getChildren() {

    load(false);
    return this.children;
  }

  /**
   * @param name the plain filename (excluding any path).
   * @param folder - {@code true} in case of a folder, {@code false} otherwise (plain data file).
   * @return {@code true} if the existing file from the file-system should be {@link #getOrCreateChild(String) created as child}, {@code false} otherwise
   *     (ignore the file).
   */
  protected boolean isAllowedChild(String name, boolean folder) {

    return folder;
  }

  /**
   * @return the {@link Set} with all {@link #getName() names} of the children.
   */
  public Set<String> getChildNames() {

    return this.childNames;
  }

  @Override
  public void load(boolean recursive) {

    if (!this.loaded) {
      Path path = getPath();
      if (Files.isDirectory(path)) {
        try (Stream<Path> childStream = Files.list(path)) {
          childStream.forEach(c -> loadChild(c, recursive));
        } catch (IOException e) {
          throw new IllegalStateException("Failed to list children of directory " + path, e);
        }
      }
      this.loaded = true;
    }
  }

  private void loadChild(Path childPath, boolean recursive) {

    String name = childPath.getFileName().toString();
    if (name.startsWith(".")) {
      return; // ignore hidden files and folders (e.g. ".git")
    }
    boolean folder = Files.isDirectory(childPath);
    if (isAllowedChild(name, folder)) {
      C child = getOrCreateChild(name);
      if (recursive) {
        child.load(recursive);
      }
    }
  }

  /**
   * @param name the {@link #getName() name} of the requested child.
   * @return the new child object.
   */
  protected abstract C newChild(String name);

  @Override
  public void save() {

    for (C child : this.childMap.values()) {
      child.save();
    }
  }
}
