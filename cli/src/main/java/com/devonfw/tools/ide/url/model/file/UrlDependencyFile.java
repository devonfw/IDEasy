package com.devonfw.tools.ide.url.model.file;

import com.devonfw.tools.ide.url.model.file.json.ToolDependencies;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;

/**
 * {@link UrlFile} for the "dependency.json" file.
 */
public class UrlDependencyFile extends AbstractUrlFile<AbstractUrlToolOrEdition<?, ?>> {

  public static final String DEPENDENCY_JSON = "dependencies.json";


  private ToolDependencies dependencies;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlDependencyFile(AbstractUrlToolOrEdition<?, ?> parent) {

    super(parent, DEPENDENCY_JSON);
  }

  /**
   * @return the content of the dependency map of the dependency.json file
   */
  public ToolDependencies getDependencies() {

    if (this.dependencies == null) {
      return ToolDependencies.getEmpty();
    }
    return this.dependencies;
  }

  @Override
  protected void doLoad() {

    this.dependencies = ToolDependencies.of(getPath());
  }

  @Override
  protected void doSave() {

  }
}
