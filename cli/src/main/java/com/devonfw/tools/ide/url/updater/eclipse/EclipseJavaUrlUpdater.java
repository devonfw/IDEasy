package com.devonfw.tools.ide.url.updater.eclipse;

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
