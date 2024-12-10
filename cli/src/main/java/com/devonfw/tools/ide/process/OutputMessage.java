package com.devonfw.tools.ide.process;

/**
 * Represent an output message that can be either from stdout or stderr.
 *
 * @param error A boolean flag that indicates whether the output message is from stdout or stderr
 * @param message A string containing the outout message
 */
public record OutputMessage(boolean error, String message) {

}
