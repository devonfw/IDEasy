package com.devonfw.ide.gui;

public class AppLauncher {

  public App app;

  public AppLauncher() {

    new Thread() {
      @Override
      public void run() {

        javafx.application.Application.launch(App.class);
      }
    }.start();
    this.app = App.waitForApp();
  }

  public static void main(String[] args) {

    new Thread() {
      @Override
      public void run() {

        javafx.application.Application.launch(App.class);
      }
    }.start();
    App app = App.waitForApp();
  }

}