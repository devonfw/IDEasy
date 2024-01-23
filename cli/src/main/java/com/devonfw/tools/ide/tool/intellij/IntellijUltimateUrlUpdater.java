package com.devonfw.tools.ide.tool.intellij;

public class IntellijUltimateUrlUpdater extends IntellijUrlUpdater {

  @Override
  protected String getEdition() {

    return "ultimate";
  }

  @Override
  IntellijJsonObject getIntellijJsonRelease(IntellijJsonObject[] releases) {

    return releases[0];
  }
}
