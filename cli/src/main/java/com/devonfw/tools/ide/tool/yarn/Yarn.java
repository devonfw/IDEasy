package com.devonfw.tools.ide.tool.yarn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link NpmBasedCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends NpmBasedCommandlet {

  private static final String YARN_LOCK = "yarn.lock";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public Path findBuildDescriptor(Path directory) {

    Path lockFile = directory.resolve(YARN_LOCK);
    if (!Files.exists(lockFile)) {
      return null; // if we do not find a yarn.lock file, we let npm take over the package.json
    }
    return super.findBuildDescriptor(directory);
  }
}
