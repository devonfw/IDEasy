package com.devonfw.ide.gui.nls;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.context.IdeGuiStateManager;

/**
 * Tests for {@link NlsService} - verifies locale switching, bundle loading, and fallback behavior.
 */
public class NlsServiceTest {

  @TempDir
  Path tempUserHome;

  @TempDir
  Path tempIdeRoot;

  private String originalUserHome;

  @BeforeEach
  public void setUp() {

    this.originalUserHome = System.getProperty("user.home");
    System.setProperty("user.home", this.tempUserHome.toString());
    IdeGuiStateManager.getInstanceOverrideRootDir(this.tempIdeRoot.toString());
  }

  @AfterEach
  public void tearDown() {

    if (this.originalUserHome == null) {
      System.clearProperty("user.home");
    } else {
      System.setProperty("user.home", this.originalUserHome);
    }
  }

  @Test
  public void testGetInstanceWithLocale() {

    NlsService service = new NlsService(Locale.ENGLISH);

    assertThat(service.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(service.getResourceBundle()).isNotNull();
    assertThat(service.get("CurrentLanguage")).isEqualTo("English (en)");
  }


  @Test
  public void testSetLocale() {

    NlsService service = new NlsService(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    assertThat(service.getLocale().getLanguage()).isEqualTo("de");
    assertThat(service.getResourceBundle()).isNotNull();
    assertThat(service.get("CurrentLanguage")).isEqualTo("Deutsch (de)");
  }


  @Test
  public void testAllLocalizationBundlesContainExactlyTheEnglishKeys() throws IOException {

    NlsService service = new NlsService(Locale.ENGLISH);
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

  @Test
  public void testLanguageDisplayShowsLocaleName() {

    NlsService service = new NlsService(Locale.ENGLISH);

    assertThat(service.getLanguageDisplayName(Locale.ENGLISH)).isEqualTo("English (en)");
    assertThat(service.getLanguageDisplayName(Locale.GERMAN)).isEqualTo("Deutsch (de)");
  }


  @Test
  public void testLocaleChangeListenerIsInvokedAndCanBeRemoved() {

    NlsService service = new NlsService(Locale.ENGLISH);
    AtomicInteger counter = new AtomicInteger();
    Runnable listener = counter::incrementAndGet;

    service.addLocaleChangeListener(listener);
    service.setLocale(Locale.GERMAN);
    assertThat(counter.get()).isEqualTo(1);

    service.removeLocaleChangeListener(listener);
    service.setLocale(Locale.ENGLISH);
    assertThat(counter.get()).isEqualTo(1);
  }

  @Test
  public void testSetLocalePersistsSelectionInUserHomeIdeProperties() throws IOException {

    NlsService service = new NlsService(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    Path propertiesFile = this.tempUserHome.resolve(".ide").resolve("ide.properties");
    assertThat(Files.exists(propertiesFile)).isTrue();

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
      properties.load(inputStream);
    }
    assertThat(properties.getProperty("IDE_OPTIONS")).isEqualTo("-Duser.language=de");
  }

  
  @Test
  public void testPersistLocaleUpdatesExistingUserLangInIdeOptions() throws IOException {

    Path userIdeFolder = this.tempUserHome.resolve(".ide");
    Files.createDirectories(userIdeFolder);
    Path userProperties = userIdeFolder.resolve("ide.properties");
    Files.writeString(userProperties, "IDE_OPTIONS=-Duser.language=de\n");

    NlsService service = new NlsService(null);
    service.setLocale(Locale.ENGLISH);

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(userProperties)) {
      properties.load(inputStream);
    }

    assertThat(properties.getProperty("IDE_OPTIONS")).isEqualTo("-Duser.language=en");
  }

  @Test
  public void testPersistLocaleAppendsWhenIdeOptionsHasOtherOptions() throws IOException {

    Path userIdeFolder = this.tempUserHome.resolve(".ide");
    Files.createDirectories(userIdeFolder);
    Path userProperties = userIdeFolder.resolve("ide.properties");
    Files.writeString(userProperties, "IDE_OPTIONS=-Dfoo=bar\n");

    NlsService service = new NlsService(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(userProperties)) {
      properties.load(inputStream);
    }

    assertThat(properties.getProperty("IDE_OPTIONS")).isEqualTo("-Dfoo=bar -Duser.language=de");
  }

  @Test
  public void testPersistLocalePreservesUnrelatedIdeOptions() throws IOException {

    Path userIdeFolder = this.tempUserHome.resolve(".ide");
    Files.createDirectories(userIdeFolder);
    Path userProperties = userIdeFolder.resolve("ide.properties");
    Files.writeString(userProperties, "IDE_OPTIONS=-Dfoo=bar -Duser.language=de\n");

    NlsService service = new NlsService(null);
    service.setLocale(Locale.ENGLISH);

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(userProperties)) {
      properties.load(inputStream);
    }

    String ideOptions = properties.getProperty("IDE_OPTIONS");
    assertThat(ideOptions).contains("-Dfoo=bar");
    assertThat(ideOptions).contains("-Duser.language=en");
    assertThat(ideOptions).doesNotContain("-Duser.language=de");
  }


  @Test
  public void testNoEmptyTranslations() throws IOException {

    NlsService service = new NlsService(Locale.ENGLISH);

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

    NlsService service = new NlsService(Locale.ENGLISH);

    assertThat(service.getAvailableLocales()).contains(Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDetectsExistingBundleFiles() {

    NlsService service = new NlsService(Locale.ENGLISH);
    //confirmed languages till now
    assertThat(service.getAvailableLocales())
        .contains(Locale.GERMAN, Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDoesNotContainAbsentLocales() {

    NlsService service = new NlsService(Locale.ENGLISH);

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
      NlsService service = new NlsService(Locale.ENGLISH);
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
      NlsService service = new NlsService(Locale.ENGLISH);
      assertThat(service.getAvailableLocales())
          .containsExactlyInAnyOrder(Locale.ENGLISH, Locale.FRENCH);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
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



