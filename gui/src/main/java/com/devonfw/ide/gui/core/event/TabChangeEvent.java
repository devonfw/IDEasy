package com.devonfw.ide.gui.core.event;

import com.devonfw.ide.gui.core.tab.TabComponent;

/** Request to open a tab or focus the one already displayed for the component's title key. */
public record TabChangeEvent(TabComponent<?> component) {

}
