package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.property.EditionProperty;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.property.ToolProperty;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * An internal {@link Commandlet} to set a tool edition.
 */
public class EditionSetCommandlet extends Commandlet {

  /** The tool to set the edition of. */
  public final ToolProperty tool;

  /** The edition to set. */
  public final EditionProperty edition;

  private final FlagProperty conf;

  private final FlagProperty home;

  private final FlagProperty workspace;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EditionSetCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.tool = add(new ToolProperty("", true, "tool"));
    this.edition = add(new EditionProperty("", true, "edition"));
    this.conf = add(new FlagProperty("--conf", false, null));
    this.home = add(new FlagProperty("--home", false, null));
    this.workspace = add(new FlagProperty("--workspace", false, null));
  }

  @Override
  public String getName() {

    return "set-edition";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    String edition = this.edition.getValue();

    if (this.conf.isTrue()) {
      commandlet.setEdition(edition, true, EnvironmentVariablesType.CONF);
    } else if (this.home.isTrue()) {
      commandlet.setEdition(edition, true, EnvironmentVariablesType.USER);
    } else if (this.workspace.isTrue()) {
      commandlet.setEdition(edition, true, EnvironmentVariablesType.WORKSPACE);
    } else {
      commandlet.setEdition(edition);
    }

  }

}
