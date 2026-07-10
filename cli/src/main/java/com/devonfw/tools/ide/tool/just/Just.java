package com.devonfw.tools.ide.tool.just;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.uv.UvBasedCommandlet;

/**
 * {@link UvBasedCommandlet} for <a href="https://github.com/casey/just">just</a>.
 */
public class Just extends UvBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link com.devonfw.tools.ide.context.IdeContext}.
   */
  public Just(IdeContext context) {
    super(context, "just", Set.of(Tag.BUILD));
  }

  @Override
  public String getPackageName() {
    return "rust-just";
  }

}
