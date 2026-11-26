package com.devonfw.tools.ide.tool.dart;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractPubCacheCommandlet;

/**
 * {@link AbstractPubCacheCommandlet} for the Dart SDK (programming language).
 * <p>
 * Dart resolves its dependencies into the Pub cache, which is shared per user ({@code ~/.pub-cache}) by default. The base class pins {@code PUB_CACHE} into the
 * per-project {@link IdeContext#getIdeHome() IDE_HOME} so that each project keeps its own dependency cache, as the
 * <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox principle</a> requires.
 */
public class Dart extends AbstractPubCacheCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Dart(IdeContext context) {

    super(context, "dart", Set.of(Tag.DART));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
