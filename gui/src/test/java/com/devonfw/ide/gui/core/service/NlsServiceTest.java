package com.devonfw.ide.gui.core.service;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;

/**
 * Tests for {@link NlsService} - verifies locale resolution, bundle loading, and persistence of the language selection.
 *
 * <p>
 * This test is isolated from the machine it runs on (see {@code documentation/contributing/junit-testing.adoc}): the {@link NlsService} is created with an
 * isolated {@link IdeTestContext} whose system environment and user home are stubbed. The user home is a per-test {@link TempDir @TempDir} created for
 * every test (and cleaned up afterwards), so the persisted {@code GUI_LANGUAGE} is written to a throw-away {@code ~/.ide/ide.properties} and can never leak
 * between tests or be influenced by the developer's real user home or any other global configuration.
 */
public class NlsServiceTest extends AbstractIdeContextTest {

  /** The name of the {@code ide.properties} file in the user home. */
  private static final String IDE_PROPERTIES_FILE = "ide.properties";

  /**
   * The throw-away user home of the isolated {@link IdeTestContext}. A fresh directory is created for every test so the persisted
   * {@code ~/.ide/ide.properties} never leaks between tests and the test works on any operating system.
   */
  @TempDir
  Path userHome;

  /** The isolated {@link IdeTestContext} that the {@link NlsService} under test persists into. */
  private IdeTestContext context;

  /**
   * Prepares an isolated {@link IdeTestContext} for every test so the test is not influenced by global configuration.
   */
  @BeforeEach
  public void setUp() {

    this.context = new IdeTestContext();
    // isolate from the real System: a decoupled, empty environment so the persisted selection is not influenced by the machine environment
    this.context.setSystem(new IdeSystemTestImpl());
    // point the isolated context at a fresh, throw-away user home (created anew per test) instead of the developer's real home
    this.context.setUserHome(this.userHome);
  }

  /**
   * @return the {@code ~/.ide/ide.properties} file of the isolated {@link #context}.
   */
  private Path getUserIdeProperties() {

    return this.userHome.resolve(".ide").resolve(IDE_PROPERTIES_FILE);
  }

  /**
   * Creates a {@link NlsService} that persists the language into the isolated {@link #context} instead of the real user home.
   */
  private NlsService newService(Locale locale) {

    return new NlsService(this.context, locale);
  }

  @ParameterizedTest
  @MethodSource("testGetInstanceWithLocaleProvider")
  public void testGetInstanceWithLocale(Locale systemLocale, Locale providedLocale, Locale expectedServiceLocale) {
    Locale.setDefault(systemLocale);
    NlsService service = newService(providedLocale);

    assertThat(service.getLocale()).isEqualTo(expectedServiceLocale);
    assertThat(service.getResourceBundle()).isNotNull();
  }

  static Stream<Arguments> testGetInstanceWithLocaleProvider() {
    return Stream.of(
        arguments(Locale.GERMANY, Locale.ENGLISH, Locale.ENGLISH),
        arguments(Locale.GERMANY, Locale.GERMAN, Locale.GERMAN),
        arguments(Locale.GERMANY, null, Locale.GERMANY),
        arguments(Locale.UK, Locale.ENGLISH, Locale.ENGLISH),
        arguments(Locale.UK, Locale.GERMAN, Locale.GERMAN),
        arguments(Locale.UK, null, Locale.UK),
        arguments(Locale.FRANCE, Locale.ENGLISH, Locale.ENGLISH),
        arguments(Locale.FRANCE, Locale.GERMAN, Locale.GERMAN),
        arguments(Locale.FRANCE, null, Locale.FRANCE)
    );
  }

  @Test
  public void testAllLocalizationBundlesContainExactlyTheEnglishKeys() throws IOException {

    NlsService service = newService(Locale.ENGLISH);
    Set<String> englishKeys = loadBundleProperties(Locale.ENGLISH).stringPropertyNames();

    for (Locale locale : service.getAvailableLocales()) {
      if (locale.equals(Locale.ENGLISH)) {
        continue;
      }
      Set<String> localeKeys = loadBundleProperties(locale).stringPropertyNames();

      Set<String> missingKeys = new HashSet<>(englishKeys);
      missingKeys.removeAll(localeKeys);
      Set<String> extraKeys = new HashSet<>(localeKeys);
      extraKeys.removeAll(englishKeys);

      assertThat(missingKeys)
          .as("Missing keys in locale %s", locale)
          .isEmpty();
      assertThat(extraKeys)
          .as("Extra keys in locale %s", locale)
          .isEmpty();
    }
  }

  @ParameterizedTest
  @MethodSource("testLanguageDisplayShowsLocaleNameProvider")
  public void testLanguageDisplayShowsLocaleName(Locale serviceLocale, Locale displayLocale, String expectedString) {
    NlsService service = newService(serviceLocale);

    assertThat(service.getLanguageDisplayName(displayLocale)).isEqualTo(expectedString);
  }

  static Stream<Arguments> testLanguageDisplayShowsLocaleNameProvider() {
    return Stream.of(
        arguments(Locale.GERMANY, Locale.ENGLISH, "Englisch (en)"),
        arguments(Locale.GERMANY, Locale.GERMAN, "Deutsch (de)"),
        arguments(Locale.UK, Locale.ENGLISH, "English (en)"),
        arguments(Locale.UK, Locale.GERMAN, "German (de)"),
        arguments(Locale.FRANCE, Locale.ENGLISH, "anglais (en)"),
        arguments(Locale.FRANCE, Locale.GERMAN, "allemand (de)")
    );
  }

  /**
   * Persisting a language has to write it as the {@code GUI_LANGUAGE} user variable into the user configuration, so {@code ide gui} can apply it to the
   * launched JVM on the next start.
   */
  @Test
  public void testSetPersistedLocaleWritesGuiLanguageToUserHomeIdeProperties() throws IOException {

    NlsService service = newService(Locale.ENGLISH);
    service.setPersistedLocale(Locale.GERMAN.toLanguageTag());

    Path propertiesFile = getUserIdeProperties();
    assertThat(Files.exists(propertiesFile)).isTrue();

    Properties properties = loadUserIdeProperties(propertiesFile);
    assertThat(properties.getProperty(NlsService.GUI_LANGUAGE)).isEqualTo("de");
  }

  /**
   * Persisting a language has to replace a previously persisted selection, not accumulate entries.
   */
  @Test
  public void testSetPersistedLocaleOverwritesPreviouslyPersistedGuiLanguage() throws IOException {

    Path propertiesFile = getUserIdeProperties();
    this.context.getFileAccess().mkdirs(propertiesFile.getParent());
    Files.writeString(propertiesFile, "GUI_LANGUAGE=de");

    NlsService service = newService(Locale.ENGLISH);
    service.setPersistedLocale(Locale.ENGLISH.toLanguageTag());

    assertThat(loadUserIdeProperties(propertiesFile).getProperty(NlsService.GUI_LANGUAGE)).isEqualTo("en");
  }

  /**
   * Persisting a language has to leave unrelated user variables (such as {@code IDE_OPTIONS}) untouched.
   */
  @Test
  public void testSetPersistedLocalePreservesUnrelatedUserVariables() throws IOException {

    Path propertiesFile = getUserIdeProperties();
    this.context.getFileAccess().mkdirs(propertiesFile.getParent());
    Files.writeString(propertiesFile, "IDE_OPTIONS=-Dfoo=bar\nGUI_LANGUAGE=de");

    NlsService service = newService(Locale.ENGLISH);
    service.setPersistedLocale(Locale.ENGLISH.toLanguageTag());

    Properties properties = loadUserIdeProperties(propertiesFile);
    assertThat(properties.getProperty(NlsService.GUI_LANGUAGE)).isEqualTo("en");
    assertThat(properties.getProperty("IDE_OPTIONS")).isEqualTo("-Dfoo=bar");
  }

  @Test
  public void testNoEmptyTranslations() throws IOException {

    NlsService service = newService(Locale.ENGLISH);

    for (Locale locale : service.getAvailableLocales()) {
      Properties props = loadBundleProperties(locale);
      for (String key : props.stringPropertyNames()) {
        assertThat(props.getProperty(key))
            .as("Translation for key '%s' in locale '%s' should not be empty", key, locale)
            .isNotBlank();
      }
    }
  }

  @Test
  public void testGetAvailableLocalesAlwaysContainsEnglish() {

    NlsService service = newService(Locale.ENGLISH);

    assertThat(service.getAvailableLocales()).contains(Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDetectsExistingBundleFiles() {

    NlsService service = newService(Locale.ENGLISH);
    //confirmed languages till now
    assertThat(service.getAvailableLocales())
        .contains(Locale.GERMAN, Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDoesNotContainAbsentLocales() {

    NlsService service = newService(Locale.ENGLISH);

    assertThat(service.getAvailableLocales())
        .doesNotContain(Locale.FRENCH, Locale.JAPANESE, Locale.forLanguageTag("zh"));
  }

  @Test
  public void testGetAvailableLocalesFromDirectory(@TempDir Path bundleRoot) throws Exception {

    Path localizationDir = bundleRoot.resolve("nls");
    Files.createDirectories(localizationDir);
    Files.createFile(localizationDir.resolve("messages.properties"));
    Files.createFile(localizationDir.resolve("messages_fr.properties"));

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(new URL[] { bundleRoot.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      NlsService service = newService(Locale.ENGLISH);
      assertThat(service.getAvailableLocales())
          .containsExactlyInAnyOrder(Locale.ENGLISH, Locale.FRENCH);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  public void testGetAvailableLocalesFromJar(@TempDir Path jarRoot) throws Exception {

    Path jarFile = jarRoot.resolve("test-bundles.jar");
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
      writeJarEntry(jos, "nls/");
      writeJarEntry(jos, "nls/messages_fr.properties");
    }

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      NlsService service = newService(Locale.ENGLISH);
      assertThat(service.getAvailableLocales())
          .containsExactlyInAnyOrder(Locale.ENGLISH, Locale.FRENCH);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private static Properties loadUserIdeProperties(Path userProperties) throws IOException {

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(userProperties)) {
      properties.load(inputStream);
    }
    return properties;
  }

  private Properties loadBundleProperties(Locale locale) throws IOException {

    String resourceName = locale.equals(Locale.ENGLISH)
        ? "nls/messages.properties"
        : "nls/messages_" + locale.toLanguageTag().replace('-', '_') + ".properties";
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    assertThat(is).as("Bundle file not found: %s", resourceName).isNotNull();
    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      Properties props = new Properties();
      props.load(reader);
      return props;
    }
  }

  private static void writeJarEntry(JarOutputStream jos, String name) throws IOException {

    jos.putNextEntry(new JarEntry(name));
    jos.write("".getBytes(StandardCharsets.UTF_8));
    jos.closeEntry();
  }

}
