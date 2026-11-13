package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliArgument;
import com.devonfw.tools.ide.cli.CliArguments;
import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.commandlet.AbstractCommandlet;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.FolderProperty;

/**
 * {@link FolderProperty} for the optional {@code --project} flag of the IDE tool commandlets. On tool commandlets the trailing arguments belong to the
 * multi-valued {@code arguments} passthrough property, so this option consumes the {@code --project} / {@code --project=<path>} token itself (issue #2821) so
 * it is neither forwarded to the IDE nor swallowed into the passthrough. A bare {@code --project} opens the current working directory;
 * {@code --project=<path>} opens the given folder (a {@code ~/} or bare {@code ~} prefix is resolved against the user home).
 */
public class ProjectFolderProperty extends FolderProperty {

  /** The declared option name. */
  public static final String NAME = "--project";

  /** The {@code --project=} form of {@link #NAME} that carries the folder to open as its value. */
  private static final String NAME_PREFIX = NAME + "=";

  /**
   * The constructor.
   */
  public ProjectFolderProperty() {

    super(NAME, false, null, false);
    assert isOption();
  }

  /**
   * Consumes the declared {@code --project} option natively (issue #2821) so it is neither forwarded to the IDE nor swallowed into the passthrough
   * {@code arguments} property; auto-completion is delegated to the base implementation (issue #2822).
   */
  @Override
  protected boolean apply(String normalizedName, CliArguments args, IdeContext context, AbstractCommandlet commandlet,
      CompletionCandidateCollector collector) {

    CliArgument argument = args.current();
    // in completion mode the option name / value candidates are offered by the base implementation
    if (argument.isCompletion()) {
      return super.apply(normalizedName, args, context, commandlet, collector);
    }
    setValue(resolveFolder(argument.getValue(), context, commandlet));
    args.next(); // consume the --project token
    return true;
  }

  /**
   * Resolves the folder to open from the given {@code --project} value.
   *
   * @param value the value part after {@code --project=}, or {@code null} for the bare {@code --project} flag.
   * @param context the {@link IdeContext}.
   * @param commandlet the {@link AbstractCommandlet} that owns this property (for error reporting).
   * @return the {@link Path} to open.
   * @throws CliException if the {@code --project=} value is empty.
   */
  private Path resolveFolder(String value, IdeContext context, AbstractCommandlet commandlet) {

    if (value == null) {
      return context.getCwd(); // bare --project -> open the current working directory
    }
    if (value.isEmpty()) {
      throw new CliException(
          "The " + NAME_PREFIX + " option of command '" + commandlet.getName() + "' is missing the folder to open. Use '" + NAME
              + "' to open the current folder.");
    }
    // the shell does not expand '~' inside '--project=<value>', so resolve it against the user home here
    if ("~".equals(value) || value.startsWith("~/")) {
      return Path.of(context.getUserHome() + value.substring(1));
    }
    return Path.of(value);
  }
}
