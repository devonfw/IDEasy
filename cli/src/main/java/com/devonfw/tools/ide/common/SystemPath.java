package com.devonfw.tools.ide.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfoImpl;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Represents the PATH variable in a structured way. The PATH contains the system path entries together with the entries for the IDEasy tools. The generic
 * system path entries are stored in a {@link List} ({@code paths}) and the tool entries are stored in a {@link Map} ({@code tool2pathMap}) as they can change
 * dynamically at runtime (e.g. if a new tool is installed). As the tools must have priority the actual PATH is build by first the entries for the tools and
 * then the generic entries from the system PATH. Such tool entries are ignored from the actual PATH of the {@link System#getenv(String) environment} at
 * construction time and are recomputed from the "software" folder. This is important as the initial {@link System#getenv(String) environment} PATH entries can
 * come from a different IDEasy project and the use may have changed projects before calling us again. Recomputing the PATH ensures side-effects from other
 * projects. However, it also will ensure all the entries to IDEasy locations are automatically managed and therefore cannot be managed manually be the
 * end-user.
 */
public class SystemPath {

  private static final Logger LOG = LoggerFactory.getLogger(SystemPath.class);

  private static final Pattern REGEX_WINDOWS_PATH = Pattern.compile("([a-zA-Z]:)?(\\\\[a-zA-Z0-9\\s_.-]+)+\\\\?");

  private final char pathSeparator;

  private final Map<String, Path> tool2pathMap;

  private final List<Path> paths;

  private final List<Path> extraPathEntries;

  private final IdeContext context;

  /**
   * Tools whose binaries are also bundled inside another (runtime) tool's install folder and would therefore be shadowed by it on the PATH.
   * <p>
   * {@code node} ships its own {@code npm}/{@code npx}/{@code corepack} inside its (flat, no {@code bin/}) install folder, so without an explicit order the
   * pristine, independently versioned {@code npm} install and the node-bundled one would compete for the same binary names in an arbitrary order
   * ({@code tool2pathMap} is a {@link HashMap}). The {@code flutter} framework likewise bundles its own {@code dart} inside its {@code bin/} folder, so the
   * pristine, independently versioned {@code dart} install and the flutter-bundled one would compete for the {@code dart} binary the same way. Ordering these
   * tools first guarantees the pristine installation deterministically wins. If another tool ever becomes independently versioned the same way, it is
   * added here.
   */
  private static final List<String> PATH_PRECEDENCE_TOOLS = List.of("npm", "dart");

  private static final List<String> EXTENSION_PRIORITY = List.of(".exe", ".cmd", ".bat", ".msi", ".ps1", "");

  /**
   * The constructor.
   *
   * @param context {@link IdeContext}.
   */
  public SystemPath(IdeContext context) {

    this(context, System.getenv(IdeVariables.PATH.getName()));
  }

  /**
   * The constructor.
   *
   * @param context {@link IdeContext}.
   * @param envPath the value of the PATH variable.
   */
  public SystemPath(IdeContext context, String envPath) {

    this(context, envPath, context.getIdeRoot(), context.getSoftwarePath());
  }

  /**
   * The constructor.
   *
   * @param context {@link IdeContext} for the output of information.
   * @param envPath the value of the PATH variable.
   * @param ideRoot the {@link IdeContext#getIdeRoot() IDE_ROOT}.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   */
  public SystemPath(IdeContext context, String envPath, Path ideRoot, Path softwarePath) {

    this(context, envPath, ideRoot, softwarePath, System.getProperty("path.separator").charAt(0), Collections.emptyList());
  }

  /**
   * The constructor.
   *
   * @param context {@link IdeContext} for the output of information.
   * @param envPath the value of the PATH variable.
   * @param ideRoot the {@link IdeContext#getIdeRoot() IDE_ROOT}.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   * @param pathSeparator the path separator char (';' for Windows and ':' otherwise).
   * @param extraPathEntries the {@link List} of additional {@link Path}s to prepend.
   */
  public SystemPath(IdeContext context, String envPath, Path ideRoot, Path softwarePath, char pathSeparator, List<Path> extraPathEntries) {

    this(context, pathSeparator, extraPathEntries, new HashMap<>(), new ArrayList<>());
    String[] envPaths = envPath.split(Character.toString(pathSeparator));
    for (String segment : envPaths) {
      Path path = parsePathSegment(segment);
      if (path == null) {
        continue;
      }
      String tool = getTool(path, ideRoot);
      if (tool == null) {
        this.paths.add(path);
      }
    }
    collectToolPath(softwarePath);
  }

  /**
   * Parses a single {@code PATH} segment into a {@link Path}. The segment is first {@link #normalizePathSegment(String) normalized} by removing illegal control
   * characters (e.g. CR, LF, NUL, TAB) that may sneak into the {@code PATH} variable (e.g. via broken tool configurations) and would otherwise render the entry
   * invalid. This way a still usable entry is kept instead of being dropped. If nothing usable remains after normalization or the segment cannot be parsed as a
   * {@link Path} even after normalization, {@code null} is returned so the entry is skipped without aborting the entire {@link SystemPath} construction.
   *
   * @param segment the raw {@code PATH} segment.
   * @return the parsed {@link Path} or {@code null} if the segment is invalid and should be ignored.
   */
  private static Path parsePathSegment(String segment) {

    String normalized = normalizePathSegment(segment);
    if (normalized.isEmpty()) {
      if (!segment.isEmpty()) {
        LOG.warn("Ignoring invalid PATH entry '{}' as it only contains illegal control characters.", segment);
      }
      return null;
    }
    if (!normalized.equals(segment)) {
      LOG.warn("Normalized PATH entry '{}' by removing illegal control characters.", segment);
    }
    try {
      return Path.of(normalized);
    } catch (InvalidPathException e) {
      LOG.warn("Ignoring invalid PATH entry '{}' - {}", segment, e.getMessage());
      return null;
    }
  }

  /**
   * @param segment the raw {@code PATH} segment.
   * @return the given {@code segment} with all {@link Character#isISOControl(char) ISO control characters} (e.g. CR, LF, NUL, TAB) removed.
   */
  private static String normalizePathSegment(String segment) {

    StringBuilder sb = new StringBuilder(segment.length());
    for (int i = 0; i < segment.length(); i++) {
      char c = segment.charAt(i);
      if (!Character.isISOControl(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * @param context {@link IdeContext} for the output of information.
   * @param softwarePath the {@link IdeContext#getSoftwarePath() software path}.
   * @param pathSeparator the path separator char (';' for Windows and ':' otherwise).
   * @param paths the {@link List} of {@link Path}s to use in the PATH environment variable.
   */
  public SystemPath(IdeContext context, Path softwarePath, char pathSeparator, List<Path> paths) {
    this(context, pathSeparator, new ArrayList<>(), new HashMap<>(), paths);
    collectToolPath(softwarePath);
  }

  private SystemPath(IdeContext context, char pathSeparator, List<Path> extraPathEntries, Map<String, Path> tool2PathMap, List<Path> paths) {

    super();
    this.context = context;
    this.pathSeparator = pathSeparator;
    this.extraPathEntries = extraPathEntries;
    this.tool2pathMap = tool2PathMap;
    this.paths = paths;
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
          String tool = child.getFileName().toString();
          if (!"extra".equals(tool) && Files.isDirectory(child)) {
            Path toolPath = child;
            Path bin = child.resolve("bin");
            if (Files.isDirectory(bin)) {
              toolPath = bin;
            }
            this.tool2pathMap.put(tool, toolPath);
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to list children of " + softwarePath, e);
      }
    }
    warnOnBinaryNameCollisions();
  }

  /**
   * Extensions (lower-cased) that mark a file as a binary on the PATH. The logical binary name is the file name without this trailing extension
   * (e.g. {@code dart.exe} and {@code dart} both represent the {@code dart} binary).
   */
  private static final Set<String> BINARY_EXTENSIONS = Set.of(".exe", ".cmd", ".bat", ".ps1");

  /**
   * Warns about binary-name collisions between tools that are not handled by {@link #PATH_PRECEDENCE_TOOLS}: if two distinct tools expose a file with the same
   * logical binary name (e.g. two tools both providing a {@code java} executable) then which one actually wins on the PATH is determined by the arbitrary
   * iteration order of {@code tool2pathMap} and cannot be guaranteed. The {@code PATH_PRECEDENCE_TOOLS} are exempt because they are ordered first and therefore
   * win deterministically over the copies bundled with the runtime that also ships them.
   */
  private void warnOnBinaryNameCollisions() {

    // Note: use the static SystemInfoImpl.INSTANCE because the context's own SystemInfo is not yet available at the point this runs (during construction).
    boolean windows = SystemInfoImpl.INSTANCE.isWindows();
    Map<String, Set<String>> toolsByBinaryName = new TreeMap<>();
    for (Map.Entry<String, Path> entry : this.tool2pathMap.entrySet()) {
      String tool = entry.getKey();
      if (PATH_PRECEDENCE_TOOLS.contains(tool) || !Files.isDirectory(entry.getValue())) {
        continue;
      }
      try (Stream<Path> files = Files.list(entry.getValue())) {
        files.filter(Files::isRegularFile).forEach(file -> {
          if (isBinaryCandidate(file, windows)) {
            String binaryName = getBinaryName(file);
            if (binaryName != null) {
              toolsByBinaryName.computeIfAbsent(binaryName, k -> new LinkedHashSet<>()).add(tool);
            }
          }
        });
      } catch (IOException e) {
        LOG.warn("Failed to list binaries of tool '{}' for binary-name collision detection - {}", tool, e.getMessage());
      }
    }
    toolsByBinaryName.forEach((binaryName, tools) -> {
      if (tools.size() > 1) {
        List<String> orderedTools = new ArrayList<>(tools);
        Collections.sort(orderedTools);
        LOG.warn("Binary name '{}' is provided by multiple tools {} and the winner on the PATH is not guaranteed. Add it to "
            + "SystemPath.PATH_PRECEDENCE_TOOLS to force a deterministic order.", binaryName, String.join(", ", orderedTools));
      }
    });
  }

  /**
   * A file counts as a binary that could be exposed on the PATH if, on Windows, it carries a launcher extension ({@code .exe}, {@code .cmd}, {@code .bat},
   * {@code .ps1}), or on other platforms it has the executable bit set. Non-executable files (e.g. a {@code readme}) and on Windows extensionless files are
   * ignored so that only genuinely launchable commands are considered.
   *
   * @param file the {@link Path} of a regular file.
   * @param windows {@code true} if running on Windows.
   * @return {@code true} if the file is a binary candidate for PATH collision detection.
   */
  private static boolean isBinaryCandidate(Path file, boolean windows) {

    if (windows) {
      String name = file.getFileName().toString().toLowerCase();
      for (String extension : BINARY_EXTENSIONS) {
        if (name.endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
    return Files.isExecutable(file);
  }

  /**
   * @param file the {@link Path} of a file.
   * @return the logical binary name of the file (the file name without a trailing executable extension, lower-cased) or {@code null} if the name is empty.
   */
  private static String getBinaryName(Path file) {

    String name = file.getFileName().toString().toLowerCase();
    for (String extension : BINARY_EXTENSIONS) {
      if (name.endsWith(extension)) {
        name = name.substring(0, name.length() - extension.length());
        break;
      }
    }
    return name.isEmpty() ? null : name;
  }

  /**
   * @return the tool bin {@link Path}s in the order in which they must be searched and placed on the PATH. The {@link #PATH_PRECEDENCE_TOOLS} are placed
   *     first so that their executables deterministically take precedence over the copies bundled with the runtime that also ships them (e.g. the pristine
   *     {@code npm} over the {@code npm}/{@code npx} bundled with {@code node}); the remaining tools are returned in their map order.
   */
  private List<Path> getToolPathsInResolutionOrder() {

    List<Path> orderedPaths = new ArrayList<>(this.tool2pathMap.size());
    for (String tool : PATH_PRECEDENCE_TOOLS) {
      Path toolPath = this.tool2pathMap.get(tool);
      if (toolPath != null) {
        orderedPaths.add(toolPath);
      }
    }
    for (Map.Entry<String, Path> entry : this.tool2pathMap.entrySet()) {
      if (!PATH_PRECEDENCE_TOOLS.contains(entry.getKey())) {
        orderedPaths.add(entry.getValue());
      }
    }
    return orderedPaths;
  }

  private static String getTool(Path path, Path ideRoot) {

    if (ideRoot == null) {
      return null;
    }
    if (path.startsWith(ideRoot)) {
      Path relativized = ideRoot.relativize(path);
      int count = relativized.getNameCount();
      if (count >= 3) {
        if (relativized.getName(1).toString().equals("software")) {
          return relativized.getName(2).toString();
        }
      }
    }
    return null;
  }

  private Path findBinaryInOrder(Path path, String tool) {

    List<String> extensionPriority = List.of("");
    if (this.context.getSystemInfo().isWindows() || SystemInfoImpl.INSTANCE.isWindows()) {
      extensionPriority = EXTENSION_PRIORITY;
    }
    for (String extension : extensionPriority) {

      Path fileToExecute = path.resolve(tool + extension);

      if (Files.exists(fileToExecute, LinkOption.NOFOLLOW_LINKS)) {
        return fileToExecute;
      }
    }

    return null;
  }

  /**
   * @param binaryName the name of the tool.
   * @return {@code true} if the given {@code tool} is a binary that can be found on the PATH, {@code false} otherwise.
   */
  public boolean hasBinaryOnPath(String binaryName) {
    Path binary = Path.of(binaryName);
    Path resolvedBinary = findBinary(binary);
    return (resolvedBinary != binary);
  }

  /**
   * @param binaryName the name of the tool.
   * @return the {@link Path} to the binary executable of the tool. E.g. if "mvn" is given then ".../software/mvn/bin/mvn" could be returned. If the executable
   *     was not found on PATH, the same {@link Path} instance is returned that was given as argument.
   */
  public Path findBinaryPathByName(String binaryName) {
    return findBinary(Path.of(binaryName));
  }

  /**
   * @param toolPath the {@link Path} to the tool installation.
   * @return the {@link Path} to the binary executable of the tool. E.g. if "mvn" is given then ".../software/mvn/bin/mvn" could be returned. If the executable
   *     was not found on PATH, the same {@link Path} instance is returned that was given as argument.
   */
  public Path findBinary(Path toolPath) {
    return findBinary(toolPath, p -> true);
  }

  /**
   * Finds a binary for {@code toolPath} but allows excluding candidates via {@code filter}. If the filter rejects a candidate, the search continues. If no
   * acceptable candidate is found, the original {@code toolPath} is returned unchanged.
   *
   * @param toolPath the {@link Path} to the tool installation or a simple name (e.g., "git").
   * @param filter a predicate that must return {@code true} for acceptable candidates; {@code false} rejects and continues searching.
   * @return the first acceptable {@link Path} found; if none, the original {@code toolPath}.
   */
  public Path findBinary(Path toolPath, Predicate<Path> filter) {
    Objects.requireNonNull(toolPath, "toolPath");
    Objects.requireNonNull(filter, "filter");

    Path parent = toolPath.getParent();
    String fileName = toolPath.getFileName().toString();

    if (parent == null) {
      for (Path path : this.extraPathEntries) {
        Path binaryPath = findBinaryInOrder(path, fileName);
        if (binaryPath != null && filter.test(binaryPath)) {
          return binaryPath;
        }
      }
      for (Path path : getToolPathsInResolutionOrder()) {
        Path binaryPath = findBinaryInOrder(path, fileName);
        if (binaryPath != null && filter.test(binaryPath)) {
          return binaryPath;
        }
      }
      for (Path path : this.paths) {
        Path binaryPath = findBinaryInOrder(path, fileName);
        if (binaryPath != null && filter.test(binaryPath)) {
          return binaryPath;
        }
      }
    } else {
      Path binaryPath = findBinaryInOrder(parent, fileName);
      if (binaryPath != null && filter.test(binaryPath)) {
        return binaryPath;
      }
    }

    return toolPath;
  }

  /**
   * @param tool the name of the tool.
   * @return the {@link Path} to the directory of the tool where the binaries can be found or {@code null} if the tool is not installed.
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

    return toString(null);
  }

  /**
   * @param pathSyntax the {@link WindowsPathSyntax} to convert to.
   * @return this {@link SystemPath} as {@link String} for the PATH environment variable.
   */
  public String toString(WindowsPathSyntax pathSyntax) {

    char separator;
    if (pathSyntax == WindowsPathSyntax.MSYS) {
      separator = ':';
    } else {
      separator = this.pathSeparator;
    }
    StringBuilder sb = new StringBuilder(128);
    for (Path path : this.extraPathEntries) {
      appendPath(path, sb, separator, pathSyntax);
    }
    for (Path path : getToolPathsInResolutionOrder()) {
      appendPath(path, sb, separator, pathSyntax);
    }
    for (Path path : this.paths) {
      appendPath(path, sb, separator, pathSyntax);
    }
    return sb.toString();
  }

  /**
   * Derive a new {@link SystemPath} from this instance with the given parameters.
   *
   * @param overriddenPath the entire PATH to override and replace the current one from this {@link SystemPath} or {@code null} to keep the current PATH.
   * @param extraPathEntries the {@link List} of additional PATH entries to add to the beginning of the PATH. May be empty to add nothing.
   * @return the new {@link SystemPath} derived from this instance with the given parameters applied.
   */
  public SystemPath withPath(String overriddenPath, List<Path> extraPathEntries) {

    if (overriddenPath == null) {
      return new SystemPath(this.context, this.pathSeparator, extraPathEntries, this.tool2pathMap, this.paths);
    } else {
      return new SystemPath(this.context, overriddenPath, null, null, this.pathSeparator, extraPathEntries);
    }
  }

  private static void appendPath(Path path, StringBuilder sb, char separator, WindowsPathSyntax pathSyntax) {

    if (sb.length() > 0) {
      sb.append(separator);
    }
    String pathString;
    if (pathSyntax == null) {
      pathString = path.toString();
    } else {
      pathString = pathSyntax.format(path);
    }
    sb.append(pathString);
  }

  /**
   * Method to validate if a given path string is a Windows path or not
   *
   * @param pathString The string to check if it is a Windows path string.
   * @return {@code true} if it is a valid windows path string, else {@code false}.
   */
  public static boolean isValidWindowsPath(String pathString) {

    return REGEX_WINDOWS_PATH.matcher(pathString).matches();
  }
}
