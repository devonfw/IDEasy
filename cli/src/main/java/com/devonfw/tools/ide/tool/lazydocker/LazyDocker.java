package com.devonfw.tools.ide.tool.lazydocker;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/jesseduffield/lazydocker">lazydocker</a>.
 */
public class LazyDocker extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public LazyDocker(IdeContext context) {

    super(context, "lazydocker", Set.of(Tag.DOCKER, Tag.ADMIN));
  }

}
