package com.devonfw.tools.ide.tool.extra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.devonfw.tools.ide.json.JsonObject;

/**
 * {@link ExtraTools} represents the {@code ide-extra-tools.json} file.
 */
public class ExtraTools implements JsonObject {

  /** The empty and immutable instance of {@link ExtraTools}. */
  public static final ExtraTools EMPTY = new ExtraTools().asImmutable();

  private final Map<String, List<ExtraToolInstallation>> tool2installationsMap;

  /**
   * The constructor.
   */
  public ExtraTools() {
    this(new HashMap<>());
  }

  private ExtraTools(Map<String, List<ExtraToolInstallation>> tool2installationsMap) {
    super();
    this.tool2installationsMap = tool2installationsMap;
  }

  /**
   * @param tool the {@link com.devonfw.tools.ide.tool.LocalToolCommandlet#getName() name} of the {@link com.devonfw.tools.ide.tool.LocalToolCommandlet}.
   * @return the {@link List} of {@link ExtraToolInstallation extra installations}. Will be empty if no extra installation is defined.
   */
  public List<ExtraToolInstallation> getExtraInstallations(String tool) {

    List<ExtraToolInstallation> result = this.tool2installationsMap.get(tool);
    if (result == null) {
      result = List.of();
    }
    return result;
  }

  /**
   * @param tool the {@link com.devonfw.tools.ide.tool.LocalToolCommandlet#getName() name} of the {@link com.devonfw.tools.ide.tool.LocalToolCommandlet}.
   * @param extraInstallation the {@link ExtraToolInstallation} to add.
   */
  public void addExtraInstallations(String tool, ExtraToolInstallation extraInstallation) {

    List<ExtraToolInstallation> list = this.tool2installationsMap.computeIfAbsent(tool, k -> new ArrayList<>());
    list.add(extraInstallation);
  }

  /**
   * @return the {@link List} of {@link #getExtraInstallations(String) contained tool names} in ascending order.
   */
  public List<String> getSortedToolNames() {

    Set<String> keys = this.tool2installationsMap.keySet();
    if (keys.isEmpty()) {
      return List.of();
    }
    List<String> tools = new ArrayList<>(keys);
    Collections.sort(tools);
    return tools;
  }

  /**
   * @return an immutable copy of this {@link ExtraTools}.
   */
  public ExtraTools asImmutable() {

    Map<String, List<ExtraToolInstallation>> newMap = new HashMap<>(this.tool2installationsMap.size());
    for (Entry<String, List<ExtraToolInstallation>> entry : this.tool2installationsMap.entrySet()) {
      newMap.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    Map<String, List<ExtraToolInstallation>> immutableMap = Map.copyOf(newMap);
    return new ExtraTools(immutableMap);
  }

}
