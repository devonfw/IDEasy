package com.devonfw.tools.ide.tool.eclipse;

/**
 * {@link EclipseUrlUpdater} for "jee" (C++) edition of Eclipse.
 */
public class EclipseJeeUrlUpdater extends EclipseUrlUpdater {
  @Override
  protected String getEdition() {

    return "jee";
  }
}
