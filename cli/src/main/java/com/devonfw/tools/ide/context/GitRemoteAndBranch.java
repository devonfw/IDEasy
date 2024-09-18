package com.devonfw.tools.ide.context;

/**
 * Pairs the remote and branch name
 *
 * @param remote the git remote name e.g. origin.
 * @param branch the branch name e.g. master.
 */
public record GitRemoteAndBranch(String remote, String branch) {

}
