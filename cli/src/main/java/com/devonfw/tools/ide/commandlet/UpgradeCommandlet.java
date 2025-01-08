package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class UpgradeCommandlet extends Commandlet {


  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public UpgradeCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
  }

  @Override
  public String getName() {

    return "upgrade";
  }

  @Override
  public void run() {

    ToolRepository mavenRepo = this.context.getMavenSoftwareRepository();
    VersionIdentifier lastVersion = mavenRepo.resolveVersion("ide", "ideasy", VersionIdentifier.of("*"));
    this.context.info("\n{}", lastVersion.toString());
    // compare installed version with the resolved Version, if resolved is newer, install it

  }

}
