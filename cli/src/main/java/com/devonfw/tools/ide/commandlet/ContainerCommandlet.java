package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.devonfw.tools.ide.context.IdeContext;

public class ContainerCommandlet extends Commandlet {

  private static final String IMAGE_NAME = "ideasy-linux";

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

      verifyDocker();

      verifySshAgent();

      Path dockerfile = Paths.get("IDEasy", "docker", "Dockerfile").toAbsolutePath();
      Path buildContext = Paths.get("IDEasy").toAbsolutePath();

      buildImage(
          toWslPath(dockerfile),
          toWslPath(buildContext));

      startContainer();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
  }

  private void verifyDocker() throws Exception {

    int exitCode = runAndWait(
        "wsl",
        "docker",
        "--version");

    if (exitCode != 0) {
      throw new IllegalStateException("Docker is not available.");
    }
  }

  private void verifySshAgent() throws Exception {

    int exitCode = runAndWait(
        "C:\\Windows\\System32\\OpenSSH\\ssh-add.exe",
        "-l");

    if (exitCode != 0) {
      throw new IllegalStateException(
          "No SSH identity loaded in Windows OpenSSH agent.");
    }
  }

  private void buildImage(String dockerfile, String context) throws Exception {

    int exitCode = runAndWait(
        "wsl",
        "docker",
        "build",
        "-t",
        IMAGE_NAME,
        "-f",
        dockerfile,
        context);

    if (exitCode != 0) {
      throw new RuntimeException(
          "Docker build failed with exit code " + exitCode);
    }
  }

  private void startContainer() throws Exception {

    new ProcessBuilder(
        "cmd",
        "/c",
        "start",
        "wsl",
        "docker",
        "run",
        "--rm",
        "-it",
        "-v",
        "/home/meshehi/.ssh:/root/.ssh:ro",
        "-v",
        "ideasy-data:/projects",
        IMAGE_NAME)
        .start();
  }

  private int runAndWait(String... command) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(command);

    pb.inheritIO();

    return pb.start().waitFor();
  }

  private String toWslPath(Path path) {

    return path.toString()
        .replace("\\", "/")
        .replace("C:", "/mnt/c");
  }
}
