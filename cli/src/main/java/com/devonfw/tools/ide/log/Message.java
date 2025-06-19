package com.devonfw.tools.ide.log;

/**
 * Message record for logging.
 *
 * @param messageTemplate template that has placeholders.
 * @param args arguments for filling the placeholders.
 */
public record Message(String messageTemplate, Object... args) {

}
