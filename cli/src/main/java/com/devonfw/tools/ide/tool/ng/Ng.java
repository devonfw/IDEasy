package com.devonfw.tools.ide.tool.ng;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://angular.dev/tools/cli">angular CLI</a>.
 */
public class Ng extends NpmBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ng(IdeContext context) {

    super(context, "ng", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public String getPackageName() {

    return "@angular/cli";
  }
}
