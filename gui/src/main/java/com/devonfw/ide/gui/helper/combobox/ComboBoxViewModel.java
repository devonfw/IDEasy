package com.devonfw.ide.gui.helper.combobox;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ComboBoxViewModel<T> {

  private final ObservableList<T> items = FXCollections.observableArrayList();
  private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();
  private final BooleanProperty isDisabled = new SimpleBooleanProperty();

  public boolean select(T item) {
    if (items.stream().anyMatch(i -> i.equals(item))) {
      selectedItem.set(item);
      return true;
    }
    return false;
  }

  public void clearSelection() {
    this.selectedItem.set(null);
  }

  public void selectFirst() {
    if (items.size() > 0) {
      selectedItem.set(items.get(0));
    }
  }

  public Optional<T> getSelectedItem() {
    return Optional.ofNullable(selectedItem.get());
  }

  public final ObservableList<T> getItems() {
    return items;
  }

  public final ObjectProperty<T> selectedItemProperty() {
    return selectedItem;
  }

  public final BooleanProperty isDisabledProperty() {
    return isDisabled;
  }

}
