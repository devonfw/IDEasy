package com.devonfw.tools.ide.tool.graalvm;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.graalvm.org/">GraalVM</a>, an advanced JDK with ahead-of-time Native Image compilation.
 */
public class GraalVm extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GraalVm(IdeContext context) {

    super(context, "graalvm", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwareExtraPath().resolve(getName());
  }

  @Override
  protected String getBinaryName() {

    return "java";
  }

}
