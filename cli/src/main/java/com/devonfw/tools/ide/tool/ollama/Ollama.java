package com.devonfw.tools.ide.tool.ollama;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;

/**
 * {@link AbstractLocalToolCommandlet} for <a href="https://ollama.com">Ollama</a>, a tool for running large language models locally.
 */
public class Ollama extends AbstractLocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ollama(IdeContext context) {

    super(context, "ollama", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
