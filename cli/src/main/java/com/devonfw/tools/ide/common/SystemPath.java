package com.devonfw.tools.ide.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Represents the PATH variable in a structured way.
 */
public class SystemPath {

  private final String envPath;

  private final char pathSeparator;

  private final Map<String, Path> tool2pathMap;

  private final List<Path> paths;

  /**
   * The constructor.
   *
   * @param envPath the value of the PATH variable.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   */
  public SystemPath(String envPath, Path softwarePath) {

    this(envPath, softwarePath, File.pathSeparatorChar);
  }

  /**
   * The constructor.
   *
   * @param envPath the value of the PATH variable.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   * @param pathSeparator the path separator char (';' for Windows and ':' otherwise).
   */
  public SystemPath(String envPath, Path softwarePath, char pathSeparator) {

    super();
    this.envPath = envPath;
    this.pathSeparator = pathSeparator;
    this.tool2pathMap = new HashMap<>();
    this.paths = new ArrayList<>();
    String[] envPaths = envPath.split(Character.toString(pathSeparator));
    for (String segment : envPaths) {
      Path path = Paths.get(segment);
      String tool = getTool(path, softwarePath);
      if (tool == null) {
        this.paths.add(path);
      } else {
        Path duplicate = this.tool2pathMap.putIfAbsent(tool, path);
        assert (duplicate != null);
      }
    }
    collectToolPath(softwarePath);
  }

  private void collectToolPath(Path softwarePath) {

    if (softwarePath == null) {
      return;
    }
    if (Files.isDirectory(softwarePath)) {
      try (Stream<Path> children = Files.list(softwarePath)) {
        Iterator<Path> iterator = children.iterator();
        while (iterator.hasNext()) {
          Path child = iterator.next();
          if (Files.isDirectory(child)) {
            Path toolPath = child;
            Path bin = child.resolve("bin");
            if (Files.isDirectory(bin)) {
              toolPath = bin;
            }
            this.tool2pathMap.put(child.getFileName().toString(), toolPath);
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list children of " + softwarePath, e);
      }
    }
  }

  private static String getTool(Path path, Path softwarePath) {

    if (softwarePath == null) {
      return null;
    }
    if (path.startsWith(softwarePath)) {
      int i = softwarePath.getNameCount();
      if (path.getNameCount() > i) {
        return path.getName(i).toString();
      }
    }
    return null;
  }

  /**
   * @param tool the name of the tool.
   * @return the {@link Path} to the directory of the tool where the binaries can be found or {@code null} if the tool
   *         is not installed.
   */
  public Path getPath(String tool) {

    return this.tool2pathMap.get(tool);
  }

  /**
   * @param tool the name of the tool.
   * @param path the new {@link #getPath(String) tool bin path}.
   */
  public void setPath(String tool, Path path) {

    this.tool2pathMap.put(tool, path);
  }

  @Override
  public String toString() {

    return toString(false);
  }

  /**
   * @param bash - {@code true} to convert the PATH to bash syntax (relevant for git-bash or cygwin on windows),
   *        {@code false} otherwise.
   * @return this {@link SystemPath} as {@link String} for the PATH environment variable.
   */
  public String toString(boolean bash) {

    char separator;
    if (bash) {
      separator = ':';
    } else {
      separator = this.pathSeparator;
    }
    StringBuilder sb = new StringBuilder(this.envPath.length() + 128);
    for (Path path : this.tool2pathMap.values()) {
      appendPath(path, sb, separator, bash);
    }
    for (Path path : this.paths) {
      appendPath(path, sb, separator, bash);
    }
    return sb.toString();
  }

  private void appendPath(Path path, StringBuilder sb, char separator, boolean bash) {

    if (sb.length() > 0) {
      sb.append(separator);
    }
    String pathString = path.toString();
    if (bash && (pathString.length() > 3) && (pathString.charAt(1) == ':')) {
      char slash = pathString.charAt(2);
      if ((slash == '\\') || (slash == '/')) {
        char drive = Character.toLowerCase(pathString.charAt(0));
        if ((drive >= 'a') && (drive <= 'z')) {
          pathString = "/" + drive + pathString.substring(2).replace('\\', '/');
        }
      }
    }
    sb.append(pathString);
  }

}
