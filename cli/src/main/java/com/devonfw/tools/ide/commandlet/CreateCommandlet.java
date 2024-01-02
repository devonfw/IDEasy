package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.PathProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link Commandlet} to create a new IDEasy instance
 */
public class CreateCommandlet extends Commandlet {

  private final PathProperty createPathProperty;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public CreateCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    createPathProperty = add(new PathProperty("", false, "createPath"));
  }

  @Override
  public String getName() {

    return "create";
  }

  @Override
  public void run() {

    Path createPath = createPathProperty.getValue();
    if (createPath == null || createPath.toString().isBlank()) {
      // pwd
      createPath = this.context.getCwd();
    }

    if (!Files.isDirectory(createPath)) {
      this.context.getFileAccess().mkdirs(createPath);
    }

    if (!this.context.getFileAccess().isEmptyDir(createPath)) {
      this.context.askToContinue("Directory is not empty, continue?");
    }

    this.context.getCommandletManager().getCommandlet(UpdateCommandlet.class).run();
  }
}
