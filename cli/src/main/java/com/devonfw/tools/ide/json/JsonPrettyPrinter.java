package com.devonfw.tools.ide.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Custom {@link DefaultPrettyPrinter} to format JSON output in sane way.
 */
public class JsonPrettyPrinter extends DefaultPrettyPrinter {

  /**
   * The constructor.
   */
  public JsonPrettyPrinter() {
    super();
    Indenter indenter = new DefaultIndenter("  ", "\n");
    indentObjectsWith(indenter);
    indentArraysWith(indenter);
    _objectFieldValueSeparatorWithSpaces = ": ";
  }

  private JsonPrettyPrinter(JsonPrettyPrinter other) {
    super(other);
  }

  @Override
  public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
    if (!_arrayIndenter.isInline()) {
      --_nesting;
    }
    if (nrOfValues > 0) {
      _arrayIndenter.writeIndentation(g, _nesting);
    }
    g.writeRaw(']');
  }

  @Override
  public DefaultPrettyPrinter createInstance() {
    return new JsonPrettyPrinter(this);
  }
}
