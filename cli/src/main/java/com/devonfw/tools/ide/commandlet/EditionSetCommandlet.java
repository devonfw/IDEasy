package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.EditionProperty;
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
  }

  @Override
  public String getName() {

    return "set-edition";
  }

  @Override
  public void run() {

    ToolCommandlet commandlet = this.tool.getValue();
    String edition = this.edition.getValue();

    commandlet.setEdition(edition);
  }

}
