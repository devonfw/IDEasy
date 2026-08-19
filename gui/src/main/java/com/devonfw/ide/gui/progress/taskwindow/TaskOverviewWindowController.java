package com.devonfw.ide.gui.progress.taskwindow;

import java.util.HashMap;
import java.util.Map;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import com.devonfw.ide.gui.context.TaskManager;
import com.devonfw.ide.gui.progress.GuiTask;

/**
 * Controller for the task overview window, which shows all tasks and their progress.
 * <p>
 * Tasks are shown in a {@link TreeView} so that a step can be expanded to reveal its sub-steps. The tree is only ever two levels deep: the hidden root holds
 * the tasks, and each task holds its sub-steps as a flat list. Whether a task is expanded lives on its {@link TreeItem}, which - unlike a cell - is never
 * recycled, so scrolling cannot move the expansion to a different row.
 */
public class TaskOverviewWindowController {

  @FXML
  private TreeView<GuiTask> taskList;

  private final TaskManager taskManager;

  /** Never shown ({@link TreeView#setShowRoot(boolean)}), it only holds the tasks as its children. */
  private final TreeItem<GuiTask> treeRoot = new TreeItem<>(null);

  /** Lets a task keep its {@link TreeItem}, and with it its expanded state, when the task list changes around it. */
  private final Map<GuiTask, TreeItem<GuiTask>> itemsByTask = new HashMap<>();

  /**
   * @param taskManager the {@link TaskManager} to link to this TaskOverviewWindow.
   */
  public TaskOverviewWindowController(TaskManager taskManager) {

    this.taskManager = taskManager;
  }

  @FXML
  private void initialize() {

    this.taskList.setShowRoot(false);
    this.taskList.setRoot(this.treeRoot);
    this.taskList.setCellFactory(new TaskWindowCellFactory(this.taskManager));

    this.taskManager.getTasks().addListener((ListChangeListener<GuiTask>) _ -> syncTasks());
    syncTasks();
  }

  /**
   * Brings the top level of the tree in line with the task list. Existing items are reused so that an expanded task stays expanded when another task is added
   * or removed next to it.
   */
  private void syncTasks() {

    this.itemsByTask.keySet().removeIf(task -> !this.taskManager.getTasks().contains(task));
    this.treeRoot.getChildren().setAll(this.taskManager.getTasks().stream().map(this::itemFor).toList());
  }

  private TreeItem<GuiTask> itemFor(GuiTask task) {

    return this.itemsByTask.computeIfAbsent(task, this::createItem);
  }

  private TreeItem<GuiTask> createItem(GuiTask task) {

    TreeItem<GuiTask> item = new TreeItem<>(task);
    // Sub-tasks are append-only, so keeping the item in sync needs no diffing - and the cells never have to observe a list themselves.
    task.getSubTasks().forEach(subTask -> item.getChildren().add(new TreeItem<GuiTask>(subTask)));
    task.getSubTasks().addListener((ListChangeListener<GuiTask>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(subTask -> item.getChildren().add(new TreeItem<GuiTask>(subTask)));
        }
      }
    });
    return item;
  }
}
