package com.devonfw.tools.ide.tool.intellij;

public class IntellijCommunityUrlUpdater extends IntellijUrlUpdater {

  @Override
  protected String getEdition() {

    return "community";
  }

  @Override
  public IntellijJsonObject getIntellijJsonRelease(IntellijJsonObject[] releases) {

    return releases[1];
  }
}
