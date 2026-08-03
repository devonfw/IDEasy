package com.devonfw.tools.ide.git.repository;

/**
 * Configuration to instruct IDEasy to create a symbolic link in the workspace top-level directory with the relative path of {@link #link()} to the cloned repo
 * with optional relative sub-path {@link #target()}. Both {@link #link()} and {@link #target()} may not traverse out of their base directory (no ".." allowed
 * to prevent hacks and misusage of this feature).
 *
 * @param link the path to the link that should be created relative to the workspace directory. Typically just the name of the link. Parent directories will
 *     be created automatically if they do not exist on link creation.
 * @param target the optional target path inside the cloned git repository.
 */
public record RepositoryLink(String link, String target) {

}
