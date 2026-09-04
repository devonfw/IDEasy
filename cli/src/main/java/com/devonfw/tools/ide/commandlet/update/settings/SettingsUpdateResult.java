package com.devonfw.tools.ide.commandlet.update.settings;

import com.devonfw.tools.ide.git.repository.RepositoryType;

/**
 * @param updateStatus resulting status of the update operation
 * @param repositoryType detected type of the repository
 * @param errorMessage error message if updateStatus = {@link SettingsUpdateStatus}.SETTINGS_UPDATE_FAILED, otherwise {@code null}
 */
public record SettingsUpdateResult(SettingsUpdateStatus updateStatus, RepositoryType repositoryType, String errorMessage) {}
