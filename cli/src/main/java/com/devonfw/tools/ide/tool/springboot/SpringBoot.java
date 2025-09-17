package com.devonfw.tools.ide.tool.springboot;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

public class SpringBoot extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public SpringBoot(IdeContext context) {

    super(context, "springboot", Set.of(Tag.JAVA, Tag.ARCHITECTURE));
  }

  @Override
  protected String getBinaryName() {
    return "spring";
  }
}
