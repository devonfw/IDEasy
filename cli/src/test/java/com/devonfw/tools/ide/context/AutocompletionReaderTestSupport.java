/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package com.devonfw.tools.ide.context;

import static org.jline.reader.LineReader.ACCEPT_LINE;
import static org.jline.reader.LineReader.BACKWARD_CHAR;
import static org.jline.reader.LineReader.BACKWARD_DELETE_CHAR;
import static org.jline.reader.LineReader.BACKWARD_KILL_WORD;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.BEGINNING_OF_LINE;
import static org.jline.reader.LineReader.COMPLETE_WORD;
import static org.jline.reader.LineReader.DOWN_HISTORY;
import static org.jline.reader.LineReader.END_OF_LINE;
import static org.jline.reader.LineReader.FORWARD_WORD;
import static org.jline.reader.LineReader.KILL_WORD;
import static org.jline.reader.LineReader.UP_HISTORY;
import static org.jline.reader.LineReader.YANK;
import static org.jline.reader.LineReader.YANK_POP;
import org.assertj.core.api.Assertions;

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
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;

/**
 * Provides support for reader and completion tests.
 * Inspired by jline3
 */
public abstract class AutocompletionReaderTestSupport extends Assertions {
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

    in = new EofPipedInputStream();
    out = new ByteArrayOutputStream();
    terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
    terminal.setSize(new Size(160, 80));
    reader = new TestLineReader(terminal, "JLine", null);
    reader.setKeyMap(LineReaderImpl.EMACS);
    mask = null;
  }

  protected void assertBuffer(final String expected, final TestBuffer buffer) throws IOException {

    assertBuffer(expected, buffer, true);
  }

  protected void assertBuffer(final String expected, final TestBuffer buffer, final boolean clear) throws IOException {

    // clear current buffer, if any
    if (clear) {
      reader.getHistory().purge();
    }
    reader.list = false;
    reader.menu = false;

    in.setIn(new ByteArrayInputStream(buffer.getBytes()));

    // run it through the reader
    // String line;
    // while ((line = reader.readLine((String) null)) != null) {
    // System.err.println("Read line: " + line);
    try {
      while (true) {
        reader.readLine(null, null, mask, null);
      }
    } catch (EndOfFileException e) {
      // noop
    }
    // while ((reader.readLine(null, null, mask, null)) != null) {
    // noop
    // }

    assertThat(reader.getBuffer().toString()).isEqualTo(expected);
  }

  private String getKeyForAction(final String key) {

    return switch (key) {
      case BACKWARD_WORD -> "\u001Bb";
      case FORWARD_WORD -> "\u001Bf";
      case BEGINNING_OF_LINE -> "\033[H";
      case END_OF_LINE -> "\u0005";
      case KILL_WORD -> "\u001Bd";
      case BACKWARD_KILL_WORD -> "\u0017";
      case ACCEPT_LINE -> "\n";
      case UP_HISTORY -> "\033[A";
      case DOWN_HISTORY -> "\033[B";
      case BACKWARD_CHAR -> "\u0002";
      case COMPLETE_WORD -> "\011";
      case BACKWARD_DELETE_CHAR -> "\010";
      case YANK -> "\u0019";
      case YANK_POP -> new String(new char[] { 27, 121 });
      default -> throw new IllegalArgumentException(key);
    };
  }

  protected class TestBuffer {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public TestBuffer(String str) {

      append(str);
    }

    @Override
    public String toString() {

      return out.toString(StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {

      return out.toByteArray();
    }

    public TestBuffer op(final String op) {

      return append(getKeyForAction(op));
    }

    public TestBuffer tab() {

      return op(COMPLETE_WORD);
    }

    public TestBuffer append(final String str) {

      for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
        append(b);
      }
      return this;
    }

    public TestBuffer append(final int i) {

      out.write((byte) i);
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

      return in != null ? in.read() : -1;
    }

    @Override
    public int available() throws IOException {

      return in != null ? in.available() : 0;
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

      list = true;
      return super.doList(possible, completed, runLoop, escaper);
    }

    @Override
    protected boolean doMenu(List<Candidate> possible, String completed,
        BiFunction<CharSequence, Boolean, CharSequence> escaper) {

      menu = true;
      return super.doMenu(possible, completed, escaper);
    }
  }
}
