package com.devonfw.tools.ide.tool.docker;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;

public class Docker extends GlobalToolCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Docker(IdeContext context) {

    super(context, "docker", Set.of(Tag.DOCKER));
  }

  /*
   * @Override public String getName() {
   * 
   * return "rancher"; }
   */
}