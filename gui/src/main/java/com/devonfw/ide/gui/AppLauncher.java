package com.devonfw.ide.gui;

/**
 * Launcher class for the App. Workaround for "Error: JavaFX runtime components are missing, and are required to run this application." Inspired by
 * <a href=https://stackoverflow.com/questions/56894627/how-to-fix-error-javafx-runtime-components-are-missing-and-are-required-to-ru>StackOverflow</a>
 */
public class AppLauncher {

  @SuppressWarnings("MissingJavadoc")
  public static void main(final String[] args) {

    App.main(args);
  }

}
