package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.BooleanProperty;

/**
 * {@link Commandlet} to print the environment variables.
 */
public class ContainerCommandlet extends Commandlet {

  private static final String IMAGE_NAME = "ideasy-linux";

  private final BooleanProperty rebuild;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public ContainerCommandlet(IdeContext context) {

    super(context);

    addKeyword("container");

    this.rebuild = add(new BooleanProperty("--rebuild", false, "-r"));
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

      if (Boolean.TRUE.equals(this.rebuild.getValue())) {
        removeImage();
      }

      if (!imageExists()) {
        buildImage(
            toWslPath(dockerfile),
            toWslPath(buildContext));
      }
      startContainer();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
  }

  private void verifyDocker() throws Exception {

    int exitCode = runAndWaitSilent(
        "wsl",
        "docker",
        "--version");

    if (exitCode != 0) {
      throw new IllegalStateException("Docker is not available.");
    }
  }

  private void verifySshAgent() throws Exception {

    int exitCode = runAndWaitSilent(
        "C:\\Windows\\System32\\OpenSSH\\ssh-add.exe",
        "-l");

    if (exitCode != 0) {

      throw new IllegalStateException(
          "No SSH identity loaded in Windows OpenSSH agent.");
    }
  }

  private boolean imageExists() throws Exception {

    return runAndWaitSilent(
        "wsl",
        "docker",
        "image",
        "inspect",
        IMAGE_NAME) == 0;
  }

  private void removeImage() throws Exception {

    runAndWaitSilent(
        "wsl",
        "docker",
        "rmi",
        "-f",
        IMAGE_NAME);
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

  private String getWslHome() throws Exception {

    Process process = new ProcessBuilder(
        "wsl",
        "sh",
        "-c",
        "printf $HOME")
        .start();

    return new String(process.getInputStream().readAllBytes()).trim();
  }

  private void startContainer() throws Exception {

    String wslHome = getWslHome();

    new ProcessBuilder(
        "cmd",
        "/c",
        "start",
        "wsl",
        "docker",
        "run",
        "--rm",
        "-it",

        "-e", "DISPLAY",
        "-e", "WAYLAND_DISPLAY",
        "-e", "XDG_RUNTIME_DIR",
        "-e", "PULSE_SERVER",

        "-v", "/tmp/.X11-unix:/tmp/.X11-unix",
        "-v", "/mnt/wslg:/mnt/wslg",

        "-v", wslHome + "/.ssh:/root/.ssh",

        "-v", "ideasy-data:/projects",
        "-v", "ideasy-home:/root",

        IMAGE_NAME)
        .start();
  }

  private int runAndWait(String... command) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(command);

    pb.inheritIO();

    return pb.start().waitFor();
  }

  private int runAndWaitSilent(String... command) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(command);

    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    pb.redirectError(ProcessBuilder.Redirect.DISCARD);

    return pb.start().waitFor();
  }

  private String toWslPath(Path path) {

    return path.toString()
        .replace("\\", "/")
        .replace("C:", "/mnt/c");
  }
}
