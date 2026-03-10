package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
import com.devonfw.tools.ide.property.EditionProperty;
import com.devonfw.tools.ide.property.EnumProperty;
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

  public final EnumProperty<EnvironmentVariablesFiles> cfg;

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
    this.cfg = add(new EnumProperty<>("--cfg", false, null, EnvironmentVariablesFiles.class));
  }

  @Override
  public String getName() {

    return "set-edition";
  }

  @Override
  protected void doRun() {

    ToolCommandlet commandlet = this.tool.getValue();
    String edition = this.edition.getValue();

    EnvironmentVariablesFiles env = this.cfg.getValue();
    commandlet.setEdition(edition, true, env);

  }

}
