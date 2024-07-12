/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package com.devonfw.tools.ide.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;

/**
 * Provides support for reader and completion tests. Inspired by jline3
 */
public abstract class AutocompletionReaderTestSupport extends AbstractIdeContextTest {

  private static final String TAB = "\011";

  protected Terminal terminal;

  protected TestLineReader reader;

  protected EofPipedInputStream in;

  protected ByteArrayOutputStream out;

  protected Character mask;

  @BeforeEach
  public void setUp() throws Exception {

    Handler ch = new ConsoleHandler();
    ch.setLevel(Level.FINEST);
    Logger logger = Logger.getLogger("org.jline");
    logger.addHandler(ch);
    // Set the handler log level
    logger.setLevel(Level.INFO);

    this.in = new EofPipedInputStream();
    this.out = new ByteArrayOutputStream();
    this.terminal = new DumbTerminal("terminal", "ansi", this.in, this.out, StandardCharsets.UTF_8);
    this.terminal.setSize(new Size(160, 80));
    this.reader = new TestLineReader(this.terminal, "JLine", null);
    this.reader.setKeyMap(LineReader.EMACS);
    this.mask = null;
  }

  protected void assertBuffer(final String expected, final TestBuffer buffer) {

    assertBuffer(expected, buffer, true);
  }

  protected void assertBuffer(final String expected, final TestBuffer buffer, final boolean clear) {

    // clear current buffer, if any
    if (clear) {
      try {
        this.reader.getHistory().purge();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to purge history.", e);
      }
    }
    this.reader.list = false;
    this.reader.menu = false;

    this.in.setIn(new ByteArrayInputStream(buffer.getBytes()));

    // run it through the reader
    try {
      while (true) {
        this.reader.readLine(null, null, this.mask, null);
      }
    } catch (EndOfFileException e) {
      // noop
    }

    assertThat(this.reader.getBuffer().toString()).isEqualTo(expected);
  }

  protected class TestBuffer {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public TestBuffer(String str) {

      append(str);
    }

    @Override
    public String toString() {

      return this.out.toString(StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {

      return this.out.toByteArray();
    }

    public TestBuffer tab() {

      return append(TAB);
    }

    public TestBuffer append(final String str) {

      for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
        append(b);
      }
      return this;
    }

    public TestBuffer append(final int i) {

      this.out.write((byte) i);
      return this;
    }
  }

  public static class EofPipedInputStream extends InputStream {

    private InputStream in;

    public void setIn(InputStream in) {

      this.in = in;
    }

    @Override
    public int read() throws IOException {

      return this.in != null ? this.in.read() : -1;
    }

    @Override
    public int available() throws IOException {

      return this.in != null ? this.in.available() : 0;
    }
  }

  public static class TestLineReader extends LineReaderImpl {

    boolean list = false;

    boolean menu = false;

    public TestLineReader(Terminal terminal, String appName, Map<String, Object> variables) {

      super(terminal, appName, variables);
    }

    @Override
    protected boolean doList(List<Candidate> possible, String completed, boolean runLoop,
        BiFunction<CharSequence, Boolean, CharSequence> escaper) {

      this.list = true;
      return super.doList(possible, completed, runLoop, escaper);
    }

    @Override
    protected boolean doMenu(List<Candidate> possible, String completed,
        BiFunction<CharSequence, Boolean, CharSequence> escaper) {

      this.menu = true;
      return super.doMenu(possible, completed, escaper);
    }
  }
}
