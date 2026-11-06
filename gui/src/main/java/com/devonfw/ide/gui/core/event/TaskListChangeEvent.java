package com.devonfw.ide.gui.core.event;

import com.devonfw.ide.gui.core.context.TaskManager;

/**
 * Event sent by the {@link TaskManager} after its list of tasks has changed (a task was added or removed). It deliberately carries no data: as events may be
 * sent concurrently from different threads, a listener has to pull the current state via {@link TaskManager#getTasks()} instead of relying on a possibly
 * outdated snapshot.
 */
public record TaskListChangeEvent() {

}
