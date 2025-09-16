package com.devonfw.tools.ide.tool.yarn;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link NpmBasedCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  protected VersionIdentifier computeInstalledVersion() {

    Path binary = Path.of(this.tool); // yarn
    Path yarnPath = this.context.getPath().findBinary(binary);
    if (yarnPath == binary) {
      return null;
    }
    String version = this.context.newProcess().runAndGetSingleOutput(this.tool, "-v");
    return VersionIdentifier.of(version);
  }

}
