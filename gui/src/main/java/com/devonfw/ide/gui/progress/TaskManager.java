package com.devonfw.ide.gui.progress;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskManager {

  private static final TaskManager INSTANCE = new TaskManager();

  public static TaskManager getInstance() {
    return INSTANCE;
  }

  private final Map<String, ProgressTask> tasks = new ConcurrentHashMap<>();

  private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();

  public interface ProgressListener {

  }
}
