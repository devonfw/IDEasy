package com.devonfw.tools.ide.process;

import java.lang.ProcessBuilder.Redirect;

/**
 * The ProcessMode defines how to start the command process and how output streams are handled using {@link ProcessBuilder}. Modes that can be used:
 * {@link #BACKGROUND} {@link #BACKGROUND_SILENT} {@link #DEFAULT} {@link #DEFAULT_CAPTURE}
 */
public enum ProcessMode {
  /**
   * The process of the command will be run like a background process. Technically the parent process will simply not await its child process and a shell is
   * used to start the process. The parent process will get the output of the child process using {@link ProcessBuilder.Redirect#INHERIT}. In Unix systems the
   * equivalent of appending an '& disown' is used to detach the subprocess from its parent process. In Unix terms, the shell will not send a SIGHUP signal but
   * the process remains connected to the terminal so that output is still received. (Only '&' is not used because it just removes awaiting but not sending of
   * SIGHUP. Using nohup would simply result in redirecting output to a nohup.out file.)
   */
  BACKGROUND {
    @Override
    public Redirect getRedirectOutput() {
      return Redirect.INHERIT;
    }

    @Override
    public Redirect getRedirectError() {
      return Redirect.INHERIT;
    }

    @Override
    public Redirect getRedirectInput() {
      return null;
    }
  },
  /**
   * Like {@link #BACKGROUND}. Instead of redirecting the output to the parent process, the output is redirected to the 'null file' using
   * {@link ProcessBuilder.Redirect#DISCARD}.
   */
  BACKGROUND_SILENT {
    @Override
    public Redirect getRedirectOutput() {
      return Redirect.DISCARD;
    }

    @Override
    public Redirect getRedirectError() {
      return Redirect.DISCARD;
    }

    @Override
    public Redirect getRedirectInput() {
      return null;
    }
  },
  /**
   * The process will be started according {@link ProcessBuilder.Redirect#INHERIT} without any detaching of parent process and child process. This setting makes
   * the child process dependant from the parent process! (If you close the parent process the child process will also be terminated.)
   */
  DEFAULT {
    @Override
    public Redirect getRedirectOutput() {
      return Redirect.INHERIT;
    }

    @Override
    public Redirect getRedirectError() {
      return Redirect.INHERIT;
    }

    @Override
    public Redirect getRedirectInput() {
      return Redirect.INHERIT;
    }

  },
  /**
   * The process will be started according {@link ProcessBuilder.Redirect#PIPE} and the standard output and standard error streams will be captured from the
   * parent process. In other words, they will be printed in the console of the parent process. This setting makes the child process dependant from the parent
   * process! (If you close the parent process the child process will also be terminated.)
   */
  DEFAULT_CAPTURE {
    @Override
    public Redirect getRedirectOutput() {
      return Redirect.PIPE;
    }

    @Override
    public Redirect getRedirectError() {
      return Redirect.PIPE;
    }

    @Override
    public Redirect getRedirectInput() {
      return null;
    }

  },

  /**
   * Like {@link #DEFAULT} the parent and child process will not be detached, the subprocess output will be discarded (to the operating system "null file" )
   * using {@link ProcessBuilder.Redirect#DISCARD}.
   */
  DEFAULT_SILENT {
    @Override
    public Redirect getRedirectOutput() {

      return Redirect.DISCARD;
    }

    @Override
    public Redirect getRedirectError() {

      return Redirect.DISCARD;
    }

    @Override
    public Redirect getRedirectInput() {

      return Redirect.INHERIT;
    }
  };


  /**
   * Method to return the {@link ProcessBuilder.Redirect} strategy for the standard output stream of the process. Depending on the mode, the output may be
   * inherited from the parent process, discarded, or piped for programmatic access.
   *
   * @return the {@link ProcessBuilder.Redirect} configuration for standard output, or {@code null} if no redirection is specified.
   */
  public abstract Redirect getRedirectOutput();

  /**
   * Method to return the {@link ProcessBuilder.Redirect} strategy for the standard error stream of the process. Depending on the mode, the error output may be
   * inherited from the parent process, discarded, or piped for programmatic access.
   *
   * @return the {@link ProcessBuilder.Redirect} configuration for standard error, or {@code null} if no redirection is specified.
   */
  public abstract Redirect getRedirectError();

  /**
   * Method to return the {@link ProcessBuilder.Redirect} strategy for the standard input stream of the process. Depending on the mode, the input may be
   * inherited from the parent process or left unconfigured.
   *
   * @return the {@link ProcessBuilder.Redirect} configuration for standard input, or {@code null} if no redirection is specified.
   */
  public abstract Redirect getRedirectInput();


  /**
   * Method to check if the ProcessMode is a background process.
   *
   * @return {@code true} if the {@link ProcessMode} is {@link ProcessMode#BACKGROUND} or {@link ProcessMode#BACKGROUND_SILENT}, {@code false} if not.
   */
  public boolean isBackground() {

    return this == BACKGROUND || this == BACKGROUND_SILENT;
  }

  // TODO ADD EXTERNAL_WINDOW_MODE IN FUTURE Issue: https://github.com/devonfw/IDEasy/issues/218

}
