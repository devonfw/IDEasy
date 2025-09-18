package com.devonfw.tools.ide.tool.spring;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/spring-projects/spring-boot">Spring-Boot-CLI</a>.
 */
public class Spring extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spring(IdeContext context) {

    super(context, "spring", Set.of(Tag.JAVA, Tag.ARCHITECTURE));
  }
}
