package com.devonfw.ide.gui.localization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LocalizationService} - verifies locale switching, bundle loading, and fallback behavior.
 */
public class LocalizationServiceTest {

  private Set<String> englishKeys;

  @BeforeEach
  public void setUp() {

    // Reset the singleton instance before each test
    LocalizationService.resetInstance();
    // Preload English bundle keys used by bundle-completion tests
    ResourceBundle englishBundle = ResourceBundle.getBundle("localization.messages", Locale.ENGLISH);
    this.englishKeys = extractKeys(englishBundle);
  }

  @Test
  public void testGetInstanceWithLocale() {

    Locale englishLocale = Locale.ENGLISH;
    LocalizationService service = LocalizationService.getInstance(englishLocale);

    assertThat(service.getLocale()).isEqualTo(englishLocale);
    assertThat(service.getResourceBundle()).isNotNull();
  }

  @Test
  public void testGetInstanceSingleton() {

    LocalizationService service1 = LocalizationService.getInstance(Locale.ENGLISH);
    LocalizationService service2 = LocalizationService.getInstance();

    assertThat(service1).isSameAs(service2);
  }

  @Test
  public void testSetLocale() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    service.setLocale(Locale.GERMAN);

    assertThat(service.getLocale().getLanguage()).isEqualTo("de");
    assertThat(service.getResourceBundle()).isNotNull();
  }


  @Test
  public void testGetMissingKeyReturnsFallback() {

    LocalizationService service = LocalizationService.getInstance(Locale.ENGLISH);
    String value = service.get("non.existent.key");

    assertThat(value).isEqualTo("!non.existent.key!");
  }


  @Test
  public void testGermanBundleCompleteWithAllEnglishKeys() {

    ResourceBundle germanBundle = ResourceBundle.getBundle("localization.messages", Locale.GERMAN);
    Set<String> germanKeys = extractKeys(germanBundle);

    assertThat(germanKeys).as("German bundle must contain all keys from English bundle").containsAll(this.englishKeys);
  }

  @Test
  public void testGermanBundleDoesNotHaveExtraKeys() {

    ResourceBundle germanBundle = ResourceBundle.getBundle("localization.messages", Locale.GERMAN);
    Set<String> germanKeys = extractKeys(germanBundle);

    assertThat(this.englishKeys).as("German bundle should not have extra keys beyond the English bundle").containsAll(germanKeys);
  }

  @Test
  public void testNoEmptyTranslations() {

    ResourceBundle englishBundle = ResourceBundle.getBundle("localization.messages", Locale.ENGLISH);
    for (String key : this.englishKeys) {
      String value = englishBundle.getString(key);
      assertThat(value).isNotBlank()
          .as("Translation for key '%s' should not be empty", key);
    }
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



