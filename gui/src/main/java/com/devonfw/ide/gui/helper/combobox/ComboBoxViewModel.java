package com.devonfw.ide.gui.helper.combobox;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * Small view model for a {@link ComboBox}, exposing the items, the selected item and a disabled flag as observable properties.
 */
public class ComboBoxViewModel<T> {

  private final ObservableList<T> items = FXCollections.observableArrayList();
  private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();
  private final BooleanProperty isDisabled = new SimpleBooleanProperty();

  /**
   * Selects the given item if it is one of the items.
   *
   * @param item the item to select.
   * @return whether the item was present and got selected.
   */
  public boolean select(T item) {
    if (items.stream().anyMatch(i -> i.equals(item))) {
      selectedItem.set(item);
      return true;
    }
    return false;
  }

  /**
   * Clears the current selection.
   */
  public void clearSelection() {
    this.selectedItem.set(null);
  }

  /**
   * Selects the first item.
   */
  public void selectFirst() {
    if (items.size() > 0) {
      selectedItem.set(items.get(0));
    }
  }

  /**
   * @return the selected item, or an empty {@link Optional} if none is selected.
   */
  public Optional<T> getSelectedItem() {
    return Optional.ofNullable(selectedItem.get());
  }

  /**
   * @return the items shown in the combo box.
   */
  public final ObservableList<T> getItems() {
    return items;
  }

  /**
   * @return the property holding the selected item.
   */
  public final ObjectProperty<T> selectedItemProperty() {
    return selectedItem;
  }

  /**
   * @return the property flagging whether the combo box is disabled.
   */
  public final BooleanProperty isDisabledProperty() {
    return isDisabled;
  }

}
