package com.devonfw.tools.ide.tool;

/**
 * Record for the combination of {@link #tool() tool name} and {@link #edition() tool edition}.
 *
 * @param tool the {@link ToolCommandlet#getName() tool name}.
 * @param edition the {@link ToolCommandlet#getConfiguredEdition() configured edition}.
 */
public record ToolEdition(String tool, String edition) {

  public ToolEdition {
    if (edition == null) {
      edition = tool;
    }
  }

  @Override
  public String toString() {

    if (this.edition.equals(this.tool)) {
      return this.tool;
    }
    return this.tool + "/" + this.edition;
  }
}
