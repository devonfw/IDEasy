package com.devonfw.tools.ide.tool.ruff;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.uv.UvBasedCommandlet;

/**
 * {@link UvBasedCommandlet} for <a href="https://github.com/astral-sh/ruff">ruff</a>.
 */
public class Ruff extends UvBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ruff(IdeContext context) {
    super(context, "ruff", Set.of(Tag.BUILD));
  }

  @Override
  public String getPackageName() {
    return "ruff";
  }

  @Override
  protected String completeRequestOption(PackageManagerRequest request) {

    if (PackageManagerRequest.TYPE_INSTALL.equals(request.getType())) {
      return "--force";
    }

    return super.completeRequestOption(request);
  }


}
