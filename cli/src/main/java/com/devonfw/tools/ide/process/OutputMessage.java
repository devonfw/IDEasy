package com.devonfw.tools.ide.process;

/**
 * Represent a log event.
 *
 * @param error A boolean flag that indicates whether the log event represents and error or standard output
 * @param message A string containing the log message
 */
public record OutputMessage(boolean error, String message) {

}
