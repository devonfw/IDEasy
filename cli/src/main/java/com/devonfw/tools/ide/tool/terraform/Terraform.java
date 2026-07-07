package com.devonfw.tools.ide.tool.terraform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for terraform CLI (terraform).
 */
public class Terraform extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Terraform(IdeContext context) {

    super(context, "terraform", Set.of(Tag.IAC));
  }

  @Override
  public String getToolHelpArguments() {

    return "-help";
  }

  /**
   * Provides Bash completion for the direct {@code terraform} command.
   * <p>
   * We use {@code TERRAFORM_HOME} instead of an absolute path so the completion follows the active project tool link. When the Terraform version changes,
   * IDEasy updates the tool link and {@code ide env --bash} refreshes the completion dynamically.
   */
  @Override
  public String getBashCompletion() {

    // Terraform uses "terraform.exe" on Windows and "terraform" on Linux/macOS.
    String executable = "terraform";
    if (this.context.getSystemInfo().isWindows()) {
      executable = "terraform.exe";
    }

    Path terraformExecutable = getToolBinPath().resolve(executable);
    if (!Files.exists(terraformExecutable)) {
      return null;
    }
    return "complete -C \"${TERRAFORM_HOME}/terraform.exe\" terraform";

  }
}
