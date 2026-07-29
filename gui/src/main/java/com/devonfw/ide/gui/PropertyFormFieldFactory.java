package com.devonfw.ide.gui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.BooleanProperty;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;

public class PropertyFormFieldFactory {

  private PropertyFormFieldFactory() {

  }

  public static javafx.scene.Node createFormField(Property<?> property, IdeContext context) {
    if (property instanceof KeywordProperty) {
      return createKeywordField((KeywordProperty) property);
    }

    if (property instanceof BooleanProperty booleanProperty) {
      return createBooleanField(booleanProperty);
    }

    return createTextField(property, context);
  }

  private static javafx.scene.Node createTextField(Property<?> property, IdeContext context) {
    String labelName = buildLabelName(property);
    Label label = new Label(labelName);
    label.setMinWidth(140);
    label.setPadding(new Insets(2, 0, 2, 0));

    TextField textField = new TextField();
    textField.setText(property.getValueAsString() != null ? property.getValueAsString() : "");

    if (property.isRequired()) {
      label.setText(labelName + " *");
    }

    HBox hbox = new HBox(5, label, textField);
    hbox.setPadding(new Insets(2, 0, 2, 0));
    hbox.setUserData(property);

    return hbox;
  }

  private static javafx.scene.Node createBooleanField(BooleanProperty booleanProperty) {

    String displayName = booleanProperty.getName();
    if (displayName.isEmpty()) {
      displayName = booleanProperty.getAlias() != null ? booleanProperty.getAlias() : "flag";
    }

    CheckBox checkbox = new CheckBox(displayName);
    checkbox.setSelected(booleanProperty.isTrue());
    checkbox.setPadding(new Insets(2, 0, 2, 0));

    checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
      booleanProperty.setValue(newVal);
    });

    return checkbox;
  }

  private static javafx.scene.Node createKeywordField(KeywordProperty keywordProperty) {
    Label label = new Label(keywordProperty.getOptionName());
    label.setStyle("-fx-font-weight: bold;");
    label.setPadding(new Insets(2, 0, 2, 0));
    return label;
  }

  private static String buildLabelName(Property<?> property) {
    String name = property.getName();
    String alias = property.getAlias();

    if (property.isOption()) {
      if (alias != null && !alias.isEmpty()) {
        return name + " (" + alias + ")";
      }
      return name;
    }

    if (name.isEmpty() && alias != null) {
      return alias;
    }
    return name;
  }
}
