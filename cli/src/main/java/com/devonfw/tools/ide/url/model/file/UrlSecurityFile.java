package com.devonfw.tools.ide.url.model.file;

import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.url.model.folder.AbstractUrlToolOrEdition;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;

/**
 * {@link UrlFile} with the security information for an {@link UrlEdition}.
 */
public class UrlSecurityFile extends AbstractUrlFile<AbstractUrlToolOrEdition<?, ?>> {

  /** {@link #getName() Name} of security file. */
  public static final String SECURITY_JSON = "security.json";

  private ToolSecurity security;

  /**
   * The constructor.
   *
   * @param parent the {@link #getParent() parent folder}.
   */
  public UrlSecurityFile(AbstractUrlToolOrEdition<?, ?> parent) {

    super(parent, SECURITY_JSON);
  }

  /**
   * @return the content of the CVE map of the security.json file
   */
  public ToolSecurity getSecurity() {

    if (this.security == null) {
      return ToolSecurity.getEmpty();
    }
    return this.security;
  }

  @Override
  protected void doLoad() {
    this.security = ToolSecurity.of(getPath());
  }

  @Override
  protected void doSave() {

  }
}
