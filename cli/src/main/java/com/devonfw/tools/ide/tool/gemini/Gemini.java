package com.devonfw.tools.ide.tool.gemini;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link com.devonfw.tools.ide.tool.AbstractToolCommandlet} for <a href="https://github.com/google-gemini/gemini-cli">Gemini CLI</a>.
 */
public class Gemini extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gemini(IdeContext context) {
    super(context, "gemini", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getPackageName() {
    return "@google/gemini-cli";
  }
}
