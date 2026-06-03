package com.devonfw.ide.gui.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing localization (i18n) in the JavaFX GUI.
 */
public class I18nService {

  private static final Logger LOG = LoggerFactory.getLogger(I18nService.class);

  private static final String BUNDLE_NAME = "i18n.messages";

  private static final String MISSING_KEY_PREFIX = "!";

  private static final String MISSING_KEY_SUFFIX = "!";

  private static I18nService instance;

  private final List<Runnable> localeListeners = new CopyOnWriteArrayList<>();

  private Locale locale;

  private ResourceBundle resourceBundle;

  private I18nService() {
    // singleton
  }

  public static synchronized I18nService getInstance(Locale locale) {

    if (instance == null) {
      instance = new I18nService();
      instance.setLocale(locale == null ? Locale.getDefault() : locale);
    }
    return instance;
  }

  public static synchronized I18nService getInstance() {

    if (instance == null) {
      return getInstance(null);
    }
    return instance;
  }

  public static synchronized void resetInstance() {

    instance = null;
  }

  public void setLocale(Locale locale) {

    this.locale = locale;
    loadBundle();
    LOG.info("Locale set to: {}", locale.getLanguage());
    for (Runnable listener : this.localeListeners) {
      try {
        listener.run();
      } catch (Exception e) {
        LOG.warn("Locale change listener threw: {}", e.getMessage());
      }
    }
  }

  public void addLocaleChangeListener(Runnable listener) {

    if (listener != null) {
      this.localeListeners.add(listener);
    }
  }

  public void removeLocaleChangeListener(Runnable listener) {
    if (listener != null) {
      this.localeListeners.remove(listener);
    }
  }

  public Locale getLocale() {

    return this.locale;
  }

  public ResourceBundle getResourceBundle() {

    return this.resourceBundle;
  }

  public String get(String key) {

    try {
      return this.resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      LOG.warn("Missing translation key: {} for locale: {}", key, this.locale);
      return MISSING_KEY_PREFIX + key + MISSING_KEY_SUFFIX;
    }
  }

  private void loadBundle() {

    try {
      this.resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, this.locale, new UTF8Control());
      LOG.debug("ResourceBundle loaded for locale: {}", this.locale);
    } catch (MissingResourceException e) {
      LOG.error("Failed to load ResourceBundle for locale: {}", this.locale, e);
      this.resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH, new UTF8Control());
    }
  }

  /**
   * UTF-8 ResourceBundle.Control to read .properties files correctly.
   */
  private static final class UTF8Control extends ResourceBundle.Control {

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
        boolean reload) throws IOException {

      String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
      try (InputStream inputStream = loader.getResourceAsStream(resourceName)) {
        if (inputStream == null) {
          return null;
        }
        return new PropertyResourceBundle(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      }
    }
  }
}
