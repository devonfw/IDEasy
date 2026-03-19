package com.devonfw.tools.ide.commandlet;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;

/**
 * Commandlet to launch the IDEasy GUI.
 */
public class GuiCommandlet extends MvnBasedLocalToolCommandlet {

  /**
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param artifact the {@link MvnArtifact}.
   * @param tags the {@link #getTags() tags}.
   */
  public GuiCommandlet(IdeContext context, String tool, MvnArtifact artifact, Set<Tag> tags) {

    super(context, tool, artifact, tags);
    addKeyword("gui");
  }


}
