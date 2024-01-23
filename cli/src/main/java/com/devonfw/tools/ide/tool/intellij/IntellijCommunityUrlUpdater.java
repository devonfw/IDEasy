package com.devonfw.tools.ide.tool.intellij;

public class IntellijCommunityUrlUpdater extends IntellijUrlUpdater {

  @Override
  protected String getEdition() {

    return getTool();
  }

  @Override
  IntellijJsonObject getIntellijJsonRelease(IntellijJsonObject[] releases) {

    return releases[1];
  }
}
