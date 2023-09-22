package com.devonfw.tools.ide.tool.eclipse;

/**
 * {@link EclipseUrlUpdater} for "java" edition of Eclipse.
 */
public class EclipseJavaUrlUpdater extends EclipseUrlUpdater {

  @Override
  protected String getEdition() {

    return "eclipse";
  }

  @Override
  protected String getEclipseEdition() {

    return "java";
  }
}
