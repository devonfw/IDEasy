package com.devonfw.tools.ide.commandlet.cleanup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edition of an IDE tool in the global software folder as discovered by the {@code cleanup} commandlet.
 * <p>
 * For example, for IntelliJ, the editions could be "community" and "ultimate". This class holds a list of
 * {@link InstalledSoftwareVersion versions} belonging to this edition.
 */
public class InstalledSoftwareEdition extends AbstractInstalledSoftwareItem {

  /** A list of {@link InstalledSoftwareVersion versions} belonging to this edition. */
  private final List<InstalledSoftwareVersion> versions;

  /**
   * Constructor.
   *
   * @param name the name of the edition.
   * @param path the installation {@link Path} of this edition.
   */
  public InstalledSoftwareEdition(String name, Path path) {

    super(name, path);
    this.versions = new ArrayList<>();
  }

  /**
   * @return the list of {@link InstalledSoftwareVersion versions} belonging to this edition.
   */
  public List<InstalledSoftwareVersion> getVersions() {

    return this.versions;
  }

  @Override
  public String toString() {

    return "InstalledSoftwareEdition[" + getName() + "]";
  }
}
