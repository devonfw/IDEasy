package com.devonfw.tools.ide.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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

  private final IdeContext context;

  private static final List<String> EXTENSION_PRIORITY = List.of(".exe", ".cmd", ".bat", ".msi", ".ps1", "");

  /**
   * The constructor.
   *
   * @param envPath the value of the PATH variable.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   * @param context {@link IdeContext} for the output of information.
   */
  public SystemPath(String envPath, Path softwarePath, IdeContext context) {

    this(envPath, softwarePath, File.pathSeparatorChar, context);
  }

  /**
   * The constructor.
   *
   * @param envPath the value of the PATH variable.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   * @param pathSeparator the path separator char (';' for Windows and ':' otherwise).
   * @param context {@link IdeContext} for the output of information.
   */
  public SystemPath(String envPath, Path softwarePath, char pathSeparator, IdeContext context) {

    super();
    this.context = context;
    this.envPath = envPath;
    this.pathSeparator = pathSeparator;
    this.tool2pathMap = new HashMap<>();
    this.paths = new ArrayList<>();
    String[] envPaths = envPath.split(Character.toString(pathSeparator));
    for (String segment : envPaths) {
      Path path = Path.of(segment);
      String tool = getTool(path, softwarePath);
      if (tool == null) {
        this.paths.add(path);
      } else {
        Path duplicate = this.tool2pathMap.putIfAbsent(tool, path);
        if (duplicate != null) {
          context.warning("Duplicated tool path for tool: {} at path: {} with duplicated path: {}.", tool, path,
              duplicate);
        }
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
            this.paths.add(0, toolPath);
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

  private Path findBinaryInOrder(Path path, String tool) {

    for (String extension : EXTENSION_PRIORITY) {

      Path fileToExecute = path.resolve(tool + extension);

      if (Files.exists(fileToExecute)) {
        return fileToExecute;
      }
    }

    return null;
  }

  /**
   * @param toolPath the {@link Path} to the tool installation.
   * @return the {@link Path} to the binary executable of the tool. E.g. is "software/mvn" is given
   *         "software/mvn/bin/mvn" could be returned.
   */
  public Path findBinary(Path toolPath) {

    Path parent = toolPath.getParent();
    String fileName = toolPath.getFileName().toString();

    if (parent == null) {
      for (Path path : this.paths) {
        Path binaryPath = findBinaryInOrder(path, fileName);
        if (binaryPath != null) {
          return binaryPath;
        }
      }
    } else {
      Path binaryPath = findBinaryInOrder(parent, fileName);
      if (binaryPath != null) {
        return binaryPath;
      }
    }

    return toolPath;
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

  private static void appendPath(Path path, StringBuilder sb, char separator, boolean bash) {

    if (sb.length() > 0) {
      sb.append(separator);
    }
    String pathString = path.toString();
    if (bash && (pathString.length() > 3) && (pathString.charAt(1) == ':')) {
      pathString = convertWindowsPathToUnixPath(pathString);
    }
    sb.append(pathString);
  }

  /**
   * Method to convert a valid Windows path string representation to its corresponding one in Unix format.
   * 
   * @param pathString The Windows path string to convert.
   * @return The converted Unix path string.
   */
  public static String convertWindowsPathToUnixPath(String pathString) {

    char slash = pathString.charAt(2);
    if ((slash == '\\') || (slash == '/')) {
      char drive = Character.toLowerCase(pathString.charAt(0));
      if ((drive >= 'a') && (drive <= 'z')) {
        pathString = "/" + drive + pathString.substring(2).replace('\\', '/');
      }
    }
    return pathString;
  }

  /**
   * Method to validate if a given path string is a Windows path or not
   * 
   * @param pathString The string to check if it is a Windows path string.
   * @return {@code true} if it is a valid windows path string, else {@code false}.
   */
  public static boolean isValidWindowsPath(String pathString) {

    String windowsFilePathRegEx = "([a-zA-Z]:)?(\\\\[a-zA-Z0-9\\s_.-]+)+\\\\?";
    return Pattern.matches(windowsFilePathRegEx, pathString);
  }
}
