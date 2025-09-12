package com.devonfw.tools.ide.io;

import java.nio.file.Path;

/**
 * A link to be created at one {@link Path} pointing to a target {@link Path}.
 *
 * @param link the {@link Path} where the link is or should be located.
 * @param target the {@link Path} where the link points to.
 * @param type the {@link PathLinkType} (symbolic or hard link).
 */
public record PathLink(Path link, Path target, PathLinkType type) {

}
