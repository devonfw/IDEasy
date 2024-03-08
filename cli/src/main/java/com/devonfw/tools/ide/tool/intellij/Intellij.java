package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    // no override needed
  }

  @Override
  public void postInstall() {
    super.postInstall();
    Path binPath = getToolPath().resolve("bin");
    if (this.context.getSystemInfo().isLinux()) {
      Path ideaShLink = binPath.resolve("idea.sh");
      Path ideaLink = binPath.resolve("idea");
      if (Files.exists(ideaShLink)) {
        this.context.getFileAccess().symlink(ideaShLink, ideaLink, true);
      }
    }
  }

}
