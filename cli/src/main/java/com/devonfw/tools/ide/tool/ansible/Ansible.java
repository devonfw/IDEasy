package com.devonfw.tools.ide.tool.ansible;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

/**
 * {@link PipBasedCommandlet} for <a href="https://docs.ansible.com/">Ansible</a>.
 * <p>
 * Ansible is a python automation and configuration-management tool distributed via PyPI. Installing the {@code ansible} package makes all its CLI entry
 * points ({@code ansible}, {@code ansible-playbook}, {@code ansible-galaxy}, ...) available.
 */
public class Ansible extends PipBasedCommandlet {

  /** The folder within the project's conf folder used as {@code ANSIBLE_HOME} (Ansible's user data: collections, roles, cache, etc.). */
  private static final String ANSIBLE_HOME_FOLDER = "ansible";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ansible(IdeContext context) {

    super(context, "ansible", Set.of(Tag.IAC, Tag.PYTHON));
  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    if (this.context.getSystemInfo().isWindows()) {
      // Ansible does not support Windows as control node (PyPI classifies it as POSIX only), fail fast before installing anything.
      throw new CliException("Ansible does not support Windows as control node. Please use WSL instead.");
    }
    return super.doInstall(request);
  }

  @Override
  protected Path getToolHomePath(ToolInstallation toolInstallation) {

    // By default «TOOL»_HOME would point to the Python installation folder (the parent tool has no dedicated folder for pip-based tools); that would leak
    // ANSIBLE_HOME into every project with Python and lose Ansible's user data (collections, roles) on Python upgrades. Point it into the project
    // instead, like Azure does with AZURE_CONFIG_DIR.
    return this.context.getConfPath().resolve(ANSIBLE_HOME_FOLDER);
  }

}
