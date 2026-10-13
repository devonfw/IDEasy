package com.devonfw.tools.ide.tool.obsidian;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;

/**
 * {@link GlobalToolCommandlet} for <a href="https://obsidian.md/">Obsidian</a>.
 */
public class Obsidian extends GlobalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Obsidian(IdeContext context) {

    super(context, "obsidian", Set.of(Tag.MARK_DOWN));
  }

  @Override
  public boolean isExtract() {

    return switch (this.context.getSystemInfo().getOs()) {
      // the Windows download is an installer executable that has to be started, not extracted
      case WINDOWS -> false;
      // the macOS download is a DMG image that has to be mounted so the *.app can be taken out of it
      case MAC -> true;
      // TODO: depends on which Linux artifact the UrlUpdater ends up publishing (see #2186)
      case LINUX -> true;
    };
  }

  @Override
  public String getWindowsRegistryAppName() {

    return "Obsidian";
  }
}
