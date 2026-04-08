package com.devonfw.tools.ide.tool.squirrelsql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link LocalToolCommandlet} for <a href="https://squirrel-sql.sourceforge.io/">SQuirreL SQL</a>.
 */
public class SquirrelSql extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(SquirrelSql.class);

  private static final String SQUIRREL_SQL = "squirrel-sql";
  private static final String SQUIRREL_SQL_BAT = SQUIRREL_SQL + ".bat";
  private static final String SQUIRREL_SQL_BASH_SCRIPT = SQUIRREL_SQL + ".sh";
  private static final String USER_DIR = "." + SQUIRREL_SQL;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public SquirrelSql(IdeContext context) {

    super(context, "squirrelsql", Set.of(Tag.DB));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  @Override
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    Path userDir = getUserDir();
    args.add("-userdir");
    args.add(userDir.toString());

    super.configureToolArgs(pc, processMode, args);
  }

  /**
   * Gets the user directory for SQuirreLSQL. Creates all necessary directories if they do not exist.
   *
   * @return the path to the user directory for SQuirreLSQL.
   */
  private Path getUserDir() {
    Path cwd = this.context.getCwd();
    Path squirrelUserDir = cwd.resolve(USER_DIR);

    try {
      Files.createDirectories(squirrelUserDir);
    } catch (Exception e) {
      LOG.warn("Could not create squirrelsql conf directory at {}: {}", squirrelUserDir, e.getMessage());
    }

    return squirrelUserDir;
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return SQUIRREL_SQL_BAT;
    } else {
      return SQUIRREL_SQL_BASH_SCRIPT;
    }
  }
}
