package com.devonfw.tools.ide.tool.squirrelsql;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link LocalToolCommandlet} for <a href="https://squirrel-sql.sourceforge.io/">SQuirreL SQL</a>.
 */
public class SquirrelSql extends LocalToolCommandlet {

  private static final String SQUIRREL_SQL = "squirrel-sql";
  private static final String SQUIRREL_SQL_BAT = SQUIRREL_SQL + ".bat";
  private static final String SQUIRREL_SQL_BASH_SCRIPT = SQUIRREL_SQL + ".sh";
  private static final String SQUIRREL_SQL_MAC_BASH_SCRIPT = SQUIRREL_SQL + "-mac.sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public SquirrelSql(IdeContext context) {

    super(context, "squirrel-sql", Set.of(Tag.DB));
  }

  @Override
  public String getToolHelpArguments() {

    return "-h";
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return SQUIRREL_SQL_BAT;
    } else if (this.context.getSystemInfo().isMac()) {
      return SQUIRREL_SQL_MAC_BASH_SCRIPT;
    } else {
      return SQUIRREL_SQL_BASH_SCRIPT;
    }
  }
}
