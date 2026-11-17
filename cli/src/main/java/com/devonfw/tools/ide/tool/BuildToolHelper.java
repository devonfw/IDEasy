package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link BuildTool}s.
 */
public final class BuildToolHelper {

  private static final Logger LOG = LoggerFactory.getLogger(BuildToolHelper.class);

  private BuildToolHelper() {

    // construction forbidden
  }

  /**
   * Searches for a wrapper file in valid projects (containing a build descriptor, e.g. {@code build.gradle} or {@code pom.xml}) and returns its path.
   *
   * @param buildTool the {@link BuildTool} whose {@link BuildTool#findBuildDescriptor(Path) build descriptor} defines a valid project.
   * @param cwd the {@link Path} to start the search from (typically the current working directory).
   * @param wrapperFileName the name of the wrapper file.
   * @return the {@link Path} to the wrapper file or {@code null} if none was found.
   */
  public static Path findWrapper(BuildTool buildTool, Path cwd, String wrapperFileName) {

    Path dir = cwd;
    while ((dir != null) && (buildTool.findBuildDescriptor(dir) != null)) {
      Path wrapper = dir.resolve(wrapperFileName);
      if (Files.exists(wrapper)) {
        LOG.debug("Using wrapper: {}", wrapper);
        return wrapper;
      }
      dir = dir.getParent();
    }
    return null;
  }
}
