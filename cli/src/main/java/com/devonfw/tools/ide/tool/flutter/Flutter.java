package com.devonfw.tools.ide.tool.flutter;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.AbstractPubCacheCommandlet;

/**
 * {@link AbstractPubCacheCommandlet} for the Flutter SDK (including the bundled Dart SDK).
 * <p>
 * Flutter resolves its dependencies into the Pub cache, which is shared per user ({@code ~/.pub-cache}) by default. The base class pins {@code PUB_CACHE} into
 * the per-project {@link IdeContext#getIdeHome() IDE_HOME} so that each project keeps its own dependency cache, as the
 * <a href="https://github.com/devonfw/IDEasy/blob/main/documentation/sandbox.adoc">sandbox principle</a> requires. This is the same Pub-cache handling as the
 * standalone {@link com.devonfw.tools.ide.tool.dart.Dart Dart} tool; both share the base class so they cannot drift apart.
 */
public class Flutter extends AbstractPubCacheCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Flutter(IdeContext context) {

    super(context, "flutter", Set.of(Tag.FLUTTER));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
