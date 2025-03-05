package com.devonfw.tools.ide.tool.pip;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.DelegatingToolCommandlet;
import com.devonfw.tools.ide.tool.python.Python;

/**
 * {@link DelegatingToolCommandlet} for <a href="https://pip.pypa.io/en/stable/">pip</a>.
 */
public class Pip extends DelegatingToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Pip(IdeContext context) {

    super(context, "pip", Set.of(Tag.PYTHON), Python.class);
  }

}
