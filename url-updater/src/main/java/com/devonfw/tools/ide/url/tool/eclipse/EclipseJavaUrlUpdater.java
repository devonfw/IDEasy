package com.devonfw.tools.ide.url.tool.eclipse;

/**
 * {@link EclipseUrlUpdater} for "java" edition of Eclipse.
 */
public class EclipseJavaUrlUpdater extends EclipseUrlUpdater {

  @Override
  // getEdition() must still return "eclipse"
  protected String getEclipseEdition() {

    return "java";
  }
}
