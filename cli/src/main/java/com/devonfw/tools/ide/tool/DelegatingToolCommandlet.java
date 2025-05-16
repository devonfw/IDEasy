package com.devonfw.tools.ide.tool;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} that delegates to another ToolCommandlet.
 */
public abstract class DelegatingToolCommandlet<D extends ToolCommandlet> extends ToolCommandlet {

  private Class<D> delegateClass;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   * @param delegateClass the {@link ToolCommandlet}.
   */
  public DelegatingToolCommandlet(IdeContext context, String tool, Set<Tag> tags, Class<D> delegateClass) {

    super(context, tool, tags);
    this.delegateClass = delegateClass;
  }

  private D getDelegate() {
    return getCommandlet(this.delegateClass);
  }

  @Override
  public final boolean install(boolean silent, ProcessContext processContext) {
    return getDelegate().install(silent, processContext);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {
    return getDelegate().getInstalledVersion();
  }

  @Override
  public String getInstalledEdition() {
    return getDelegate().getInstalledEdition();
  }

  @Override
  public void uninstall() {
    getDelegate().uninstall();
  }

  @Override
  public void listEditions() {
    getDelegate().listEditions();
  }

  @Override
  public void listVersions() {
    getDelegate().listVersions();
  }

  @Override
  public void setVersion(VersionIdentifier version, boolean hint, EnvironmentVariablesFiles destination) {
    getDelegate().setVersion(version, hint, destination);
  }

  @Override
  public void setEdition(String edition, boolean hint, EnvironmentVariablesFiles destination) {
    getDelegate().setEdition(edition, hint, destination);
  }
}
