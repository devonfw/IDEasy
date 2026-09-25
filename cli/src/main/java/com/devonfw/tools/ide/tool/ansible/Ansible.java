package com.devonfw.tools.ide.tool.ansible;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.pip.PipBasedCommandlet;

/**
 * {@link PipBasedCommandlet} for <a href="https://docs.ansible.com/">Ansible</a>.
 * <p>
 * Ansible is a python automation and configuration-management tool distributed via PyPI. Installing the {@code ansible} package makes all its CLI entry
 * points ({@code ansible}, {@code ansible-playbook}, {@code ansible-galaxy}, ...) available.
 */
public class Ansible extends PipBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ansible(IdeContext context) {

    super(context, "ansible", Set.of(Tag.CONFIG_MANAGEMENT, Tag.PYTHON));
  }

}
