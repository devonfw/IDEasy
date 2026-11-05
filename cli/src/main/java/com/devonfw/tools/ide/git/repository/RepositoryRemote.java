package com.devonfw.tools.ide.git.repository;

/**
 * Configuration for an additional git remote to add to a cloned repository.
 *
 * @param name the name of the remote (e.g. "upstream").
 * @param url the URL of the remote repository.
 */
public record RepositoryRemote(String name, String url) {

}
