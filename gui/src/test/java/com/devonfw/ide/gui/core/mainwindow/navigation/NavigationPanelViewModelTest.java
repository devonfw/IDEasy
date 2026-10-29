package com.devonfw.ide.gui.core.mainwindow.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;

/**
 * Tests for {@link NavigationPanelViewModel}.
 * <p>
 * These run without starting the JavaFX toolkit. The project→workspace→context orchestration is driven by the view (see {@link NavigationPanelView}), so these
 * tests exercise the view model's own public methods the way the view calls them.
 */
public class NavigationPanelViewModelTest extends UIBasedApplicationTest {

  private static final String PROJECT = "project-1";

  private static final String OTHER_PROJECT = "project-2";

  private static final String DEFAULT_WORKSPACE = "main";

  /**
   * Throw-away user home for the isolated NlsService context so persisting the language never rewrites the real {@code ~/.ide/ide.properties}.
   */
  @TempDir
  private Path nlsUserHome;

  private GuiStateManager guiStateManager;

  private NavigationPanelViewModel viewModel;

  @BeforeEach
  void setUp() {

    setTestContext("testProject", PROJECT);

    // The active locale is fixed for the NlsService's lifetime from the JVM default at construction time, so pin it to English for stable, locale-independent
    // assertions.
    Locale.setDefault(Locale.ENGLISH);

    // isolate the NlsService from the developer's real user home so persisting the language is written to the throw-away home instead of the real
    // ~/.ide/ide.properties
    final IdeTestContext nlsContext = new IdeTestContext();
    nlsContext.setSystem(new IdeSystemTestImpl());
    nlsContext.setUserHome(this.nlsUserHome);

    this.guiStateManager = new GuiStateManager(getTestContext().getIdeRoot().toString(), nlsContext);
    this.viewModel = new NavigationPanelViewModel(this.guiStateManager);
  }

  /**
   * Populating the workspaces for a selected project has to include {@value #DEFAULT_WORKSPACE} and preselect it.
   */
  @Test
  void testPopulatingWorkspacesPreselectsMainWorkspace() {

    this.viewModel.getProjectsViewModel().select(PROJECT);
    this.viewModel.populateWorkspaceComboBox();

    assertThat(this.viewModel.getWorkspacesViewModel().getItems()).contains(DEFAULT_WORKSPACE);
    assertThat(this.viewModel.getWorkspacesViewModel().getSelectedItem()).contains(DEFAULT_WORKSPACE);
  }

  /**
   * Updating the context for a selected project/workspace has to switch the {@link GuiStateManager} to the corresponding working directory.
   */
  @Test
  void testUpdatingContextSwitchesToTheSelectedProjectAndWorkspace() {

    this.viewModel.getProjectsViewModel().select(OTHER_PROJECT);
    this.viewModel.populateWorkspaceComboBox();
    this.viewModel.updateContext();

    assertThat(this.guiStateManager.isWorkspaceSelected()).isTrue();
    assertThat(this.guiStateManager.getCurrentContext().getCwd()).endsWith(Path.of(OTHER_PROJECT, "workspaces", DEFAULT_WORKSPACE));
  }

  /**
   * Selecting a different language has to persist it as the {@code GUI_LANGUAGE} user variable and raise the restart-required flag, so the view can prompt the
   * user to restart the GUI for the change to take effect. The combo selection is owned by the view's bidirectional binding, so it is set before the change
   * handler (and thus {@code selectLanguage}) runs, exactly as in the real flow.
   */
  @Test
  void testSelectingADifferentLanguagePersistsItAndRequiresRestart() throws IOException {

    String germanLabel = getLanguageLabel(Locale.GERMAN);
    this.viewModel.getLanguagesViewModel().select(germanLabel);
    this.viewModel.selectLanguage(germanLabel);

    assertThat(loadUserProperties().getProperty(NlsService.GUI_LANGUAGE)).isEqualTo("de");
    assertThat(this.viewModel.isRestartRequired()).isTrue();
    assertThat(this.viewModel.getLanguagesViewModel().getSelectedItem()).contains(germanLabel);
  }

  /**
   * Selecting the language that is already active must be a no-op: no restart is required and the selection is left as-is (prevents re-prompt loops).
   */
  @Test
  void testSelectingTheActiveLanguageIsANoOp() throws IOException {

    String englishLabel = getLanguageLabel(Locale.ENGLISH);
    this.viewModel.selectLanguage(englishLabel);

    assertThat(this.viewModel.isRestartRequired()).isFalse();
    assertThat(this.nlsUserHome.resolve(".ide").resolve("ide.properties")).doesNotExist();
  }

  /**
   * The applied locale the GUI boots with is often a regional variant (e.g. {@code de-DE}) while the available bundles are language-only (e.g. {@code de}).
   * The combo has to resolve it to the matching language entry, not fall back to the first (English) entry, or it would disagree with the displayed language.
   * (Regression: a German GUI labelled itself as English.)
   */
  @Test
  void testInitialSelectionResolvesRegionalJvmDefaultToTheMatchingLanguageEntry() {

    Locale original = Locale.getDefault();
    try {
      // the GUI boots with the persisted language applied to the JVM; on a German machine the default is the regional variant de-DE (NOT de, as in
      // Locale.GERMANY) while the available bundles are language-only, so only the language-only fallback can resolve it
      Locale.setDefault(new Locale("de", "DE"));
      NavigationPanelViewModel regionalViewModel = newViewModelUnderCurrentDefaultLocale();

      String germanLabel = getLabelForLanguage(regionalViewModel, Locale.GERMAN.getLanguage());

      // the selected label is the German entry, not the first (English) entry the old exact-match-only logic would have fallen back to
      assertThat(regionalViewModel.getLanguagesViewModel().getSelectedItem()).contains(germanLabel);
    } finally {
      Locale.setDefault(original);
    }
  }

  /**
   * Builds a fresh view model whose {@link NlsService} picks up the current JVM default locale (as set by the caller) instead of the English one pinned in
   * {@link #setUp()}, so a regional boot locale (e.g. {@code de-DE}) can be exercised.
   */
  private NavigationPanelViewModel newViewModelUnderCurrentDefaultLocale() {

    final IdeTestContext nlsContext = new IdeTestContext();
    nlsContext.setSystem(new IdeSystemTestImpl());
    nlsContext.setUserHome(this.nlsUserHome);
    return new NavigationPanelViewModel(new GuiStateManager(getTestContext().getIdeRoot().toString(), nlsContext));
  }

  /**
   * Resolves the language combo label of the given view model by the stable language code in parentheses (independent of the localized display language).
   */
  private String getLabelForLanguage(NavigationPanelViewModel viewModel, String languageCode) {

    return viewModel.getLanguagesViewModel().getItems().stream()
        .filter(label -> label.endsWith("(" + languageCode + ")"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No language label found for language code " + languageCode));
  }

  /**
   * Resolves the language combo label for the given locale. Labels are formatted as {@code <display> (<language code>)} by {@code NlsService}, so the language
   * code in parentheses identifies the entry.
   */
  private String getLanguageLabel(Locale locale) {

    String marker = "(" + locale.getLanguage() + ")";
    return this.viewModel.getLanguagesViewModel().getItems().stream().filter(label -> label.contains(marker)).findFirst()
        .orElseThrow(() -> new IllegalStateException("No language label found for " + locale));
  }

  private Properties loadUserProperties() throws IOException {

    Path propertiesFile = this.nlsUserHome.resolve(".ide").resolve("ide.properties");
    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
      properties.load(inputStream);
    }
    return properties;
  }

}
