package com.devonfw.ide.gui.localization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
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
 * Tests for {@link LocalizationService} - verifies locale switching, bundle loading, and fallback behavior.
 */
public class LocalizationServiceTest {

  @TempDir
  Path tempUserHome;

  @TempDir
  Path tempIdeRoot;

  private String originalUserHome;

  @BeforeEach
  public void setUp() {

    this.originalUserHome = System.getProperty("user.home");
    System.setProperty("user.home", this.tempUserHome.toString());

    LocalizationService.resetInstance();
    IdeGuiStateManager.getInstanceOverrideRootDir(this.tempIdeRoot.toString());
  }

  @AfterEach
  public void tearDown() {

    LocalizationService.resetInstance();
    if (this.originalUserHome == null) {
      System.clearProperty("user.home");
    } else {
      System.setProperty("user.home", this.originalUserHome);
    }
  }

  @Test
  public void testGetInstanceWithLocale() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);

    assertThat(service.getLocale()).isEqualTo(Locale.ENGLISH);
    assertThat(service.getResourceBundle()).isNotNull();
    assertThat(service.get("CurrentLanguage")).isEqualTo("English (en)");
  }

  @Test
  public void testGetInstanceSingleton() {

    LocalizationService service1 = LocalizationService.getInstance(Locale.ENGLISH);
    LocalizationService service2 = LocalizationService.getInstance(Locale.ENGLISH);

    assertThat(service1).isSameAs(service2);
  }

  @Test
  public void testSetLocale() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    assertThat(service.getLocale().getLanguage()).isEqualTo("de");
    assertThat(service.getResourceBundle()).isNotNull();
    assertThat(service.get("CurrentLanguage")).isEqualTo("Deutsch (de)");
  }


  @Test
  public void testAllLocalizationBundlesContainExactlyTheEnglishKeys() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    Set<String> englishKeys = extractKeys(ResourceBundle.getBundle("localization.messages", Locale.ENGLISH));

    for (Locale locale : service.getAvailableLocales()) {
      ResourceBundle bundle = ResourceBundle.getBundle("localization.messages", locale);
      Set<String> localeKeys = extractKeys(bundle);

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

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);

    assertThat(service.getLanguageDisplayName(Locale.ENGLISH)).isEqualTo("English (en)");
    assertThat(service.getLanguageDisplayName(Locale.GERMAN)).isEqualTo("Deutsch (de)");
  }


  @Test
  public void testLocaleChangeListenerIsInvokedAndCanBeRemoved() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
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

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    Path propertiesFile = this.tempUserHome.resolve(".ide").resolve("ide.properties");
    assertThat(Files.exists(propertiesFile)).isTrue();

    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
      properties.load(inputStream);
    }

    assertThat(properties.getProperty("IDE_LOCALE")).isEqualTo("de");
  }

  @Test
  public void testPersistedLocaleIsLoadedFromIdePropertiesFixture() throws IOException {
    Path userIdeFolder = this.tempUserHome.resolve(".ide");
    Files.createDirectories(userIdeFolder);
    Path userProperties = userIdeFolder.resolve("ide.properties");
    Files.writeString(userProperties, "IDE_LOCALE=de\n");

    LocalizationService.resetInstance();
    LocalizationService service = LocalizationService.getInstance(null);

    assertThat(service.getLocale().getLanguage()).isEqualTo("de");
  }


  @Test
  public void testGetMissingKeyReturnsFallback() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    String value = service.get("non.existent.key");

    assertThat(value).isEqualTo("!non.existent.key!");
  }


  @Test
  public void testNoEmptyTranslations() {

    ResourceBundle englishBundle = ResourceBundle.getBundle("localization.messages", Locale.ENGLISH);
    for (String key : extractKeys(englishBundle)) {
      String value = englishBundle.getString(key);
      assertThat(value).isNotBlank()
          .as("Translation for key '%s' should not be empty", key);
    }
  }


  @Test
  public void testGetAvailableLocalesAlwaysContainsEnglish() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);

    assertThat(service.getAvailableLocales()).contains(Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDetectsExistingBundleFiles() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    //confirmed languages till now
    assertThat(service.getAvailableLocales())
        .contains(Locale.GERMAN, Locale.ENGLISH);
  }

  @Test
  public void testGetAvailableLocalesDoesNotContainAbsentLocales() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);

    assertThat(service.getAvailableLocales())
        .doesNotContain(Locale.FRENCH, Locale.JAPANESE, Locale.forLanguageTag("zh"));
  }


  @Test
  public void testGetAvailableLocalesFromDirectory(@TempDir Path bundleRoot) throws Exception {

    Path localizationDir = bundleRoot.resolve("localization");
    Files.createDirectories(localizationDir);
    Files.createFile(localizationDir.resolve("messages.properties"));
    Files.createFile(localizationDir.resolve("messages_fr.properties"));

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(new URL[] { bundleRoot.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
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
      writeJarEntry(jos, "localization/");
      writeJarEntry(jos, "localization/messages_fr.properties");
    }

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader loader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
      assertThat(service.getAvailableLocales())
          .containsExactlyInAnyOrder(Locale.ENGLISH, Locale.FRENCH);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }


  private static void writeJarEntry(JarOutputStream jos, String name) throws IOException {

    jos.putNextEntry(new JarEntry(name));
    jos.write("".getBytes(StandardCharsets.UTF_8));
    jos.closeEntry();
  }

  private Set<String> extractKeys(ResourceBundle bundle) {

    Set<String> keys = new HashSet<>();
    Enumeration<String> enumeration = bundle.getKeys();
    while (enumeration.hasMoreElements()) {
      keys.add(enumeration.nextElement());
    }
    return keys;
  }


}



