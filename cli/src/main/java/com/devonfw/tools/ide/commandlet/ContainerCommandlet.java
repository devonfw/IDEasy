package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;

public class ContainerCommandlet extends Commandlet {

  public ContainerCommandlet(IdeContext context) {
    super(context);
    addKeyword("container");
  }

  @Override
  public String getName() {
    return "container";
  }

  @Override
  protected void doRun() {

    try {

      ProcessBuilder pb = new ProcessBuilder(
          "C:\\Windows\\System32\\OpenSSH\\ssh-add.exe",
          "-l");

      pb.inheritIO();

      int exitCode = pb.start().waitFor();

      System.out.println("Exit code=" + exitCode);


    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
