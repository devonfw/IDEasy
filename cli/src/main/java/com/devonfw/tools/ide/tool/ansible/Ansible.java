package com.devonfw.tools.ide.tool.ansible;

import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.PackageManagerRequest;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

/**
 * {@link PipBasedCommandlet} for <a href="https://docs.ansible.com/">Ansible</a>.
 * <p>
 * Ansible is a python automation and configuration-management tool distributed via PyPI. Installing the {@code ansible} package makes all its CLI entry
 * points ({@code ansible}, {@code ansible-playbook}, {@code ansible-galaxy}, ...) available.
 */
public class Ansible extends PipBasedCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Ansible.class);

  /**
   * The name of the PyPI package that provides the actual Ansible binaries; the {@code ansible} package itself is only a meta package.
   * @see <a href="https://github.com/ansible/ansible/issues/73811">ansible/ansible#73811</a>
   */
  private static final String ANSIBLE_CORE_PACKAGE = "ansible-core";

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

  @Override
  public void uninstall() {

    boolean installed = isInstalled();
    super.uninstall();
    if (installed) {
      // The ansible package is only a meta package; without this the binaries of ansible-core would remain although 'ide uninstall ansible' reports
      // success.
      PackageManagerRequest request = new PackageManagerRequest(PackageManagerRequest.TYPE_UNINSTALL, ANSIBLE_CORE_PACKAGE);
      ProcessResult result = runPackageManager(request);
      if (!result.isSuccessful()) {
        // ansible-core might not be installed (e.g. if it was manually removed); do not fail the overall uninstall because of that.
        LOG.warn("Failed to uninstall the package {}.", ANSIBLE_CORE_PACKAGE);
      }
    }
  }

}
