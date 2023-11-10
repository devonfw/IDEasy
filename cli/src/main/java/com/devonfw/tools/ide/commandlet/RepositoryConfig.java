package com.devonfw.tools.ide.commandlet;

public record RepositoryConfig(
    String path,
    String workingSets,
    String workspace,
    String gitUrl,
    String gitBranch,
    String buildCmd,
    String eclipse,
    boolean active) {
}
