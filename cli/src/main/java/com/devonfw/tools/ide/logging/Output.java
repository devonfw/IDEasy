package com.devonfw.tools.ide.logging;

/**
 * Simple wrapper to print out to console and logger.
 */
public class Output {

  private static final String BANNER = "***********************************************************";

  private static final Output INSTANCE = new Output();

  /**
   * Print {@link #info(String) info} surrounded with a banner.
   *
   * @param message the message text/template.
   * @param args the optional arguments to fill into the message.
   */
  public void banner(String message, Object... args) {

    info(BANNER);
    info(message, args);
    info(BANNER);
  }

  /**
   * Print the message to console and log.
   *
   * @param message the message text/template.
   * @param args the optional arguments to fill into the message.
   */
  public void info(String message, Object... args) {

    info(String.format(message, args));
  }

  /**
   * Print the message to console and log.
   *
   * @param message the plain message.
   */
  public void info(String message) {

    System.out.println(message);
    Log.info(message);
  }

  /**
   * Log debug message.
   *
   * @param message the plain message.
   */
  public void debug(String message) {

    Log.debug(message);
  }

  /**
   * Print the message as warning to console and log.
   *
   * @param message the message text/template.
   * @param args the optional arguments to fill into the message.
   */
  public void warn(String message, Object... args) {

    warn(String.format(message, args));
  }

  /**
   * Print the message as warning to console and log.
   *
   * @param message the plain message.
   */
  public void warn(String message) {

    System.out.println("WARNING: " + message);
    Log.warn(message);
  }

  /**
   * Print the error to console and log.
   *
   * @param e the {@link Exception}.
   */
  public void err(Exception e) {

    err(e.toString(), e);
  }

  /**
   * Print the message as error to console and log.
   *
   * @param message the message text/template.
   * @param args the optional arguments to fill into the message.
   */
  public void err(String message, Object... args) {

    err(String.format(message, args), (Exception) null);
  }

  /**
   * Print the message as error to console and log.
   *
   * @param message the plain message.
   * @param e the {@link Exception} or {@code null} for none.
   */
  public void err(String message, Exception e) {

    System.out.println("ERROR: " + message);
    Log.err(message, e);
  }

  /**
   * @return get the instance of {@link Output}.
   */
  public static Output get() {

    return INSTANCE;
  }

}
