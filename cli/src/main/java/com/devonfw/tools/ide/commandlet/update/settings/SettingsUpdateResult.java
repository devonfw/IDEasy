package com.devonfw.tools.ide.commandlet.update.settings;

import com.devonfw.tools.ide.git.repository.RepositoryType;

public record SettingsUpdateResult(SettingsUpdateStatus updateStatus, RepositoryType repositoryType, String errorMessage) {

}
