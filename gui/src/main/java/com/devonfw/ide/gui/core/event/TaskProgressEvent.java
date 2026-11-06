package com.devonfw.ide.gui.core.event;

import com.devonfw.ide.gui.core.context.TaskManager;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;

/**
 * Event sent by the {@link TaskManager} after the progress (or the indeterminate state) of a running {@link ProgressBarTask} has changed. It is sent for every
 * progress step, so listeners updating the UI should coalesce these events (see {@link com.devonfw.ide.gui.core.helper.CoalescingFxRunner}) and read the
 * current progress from the task.
 *
 * @param task the {@link ProgressBarTask} whose progress has changed.
 */
public record TaskProgressEvent(ProgressBarTask task) {

}
