package com.devonfw.ide.gui.nls;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeContextConsole;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;

/**
 * Service for managing nls (i18n) in the JavaFX GUI.
 *
 * <p>Locale bundles are discovered from {@code nls/messages[_<lang>].properties}.
 * English uses the suffix-less file {@code messages.properties}. To add a new locale, add a new bundle file with the same key set and a matching suffix (for
 * example {@code messages_fr.properties}).
 */
public class NlsService {

  private static final Logger LOG = LoggerFactory.getLogger(NlsService.class);


  private static final String BUNDLE_PACKAGE = "nls";

  private static final String BUNDLE_BASE_NAME = "messages";

  private static final String BUNDLE_NAME = BUNDLE_PACKAGE + "." + BUNDLE_BASE_NAME;

  private static final String LANGUAGE_DISPLAY_KEY = "CurrentLanguage";

  public static final String EXTENSION_PROPERTIES = ".properties";

  public static final String IDE_OPTIONS = "IDE_OPTIONS";

  private final List<Runnable> localeListeners = new CopyOnWriteArrayList<>();

  private Locale locale;

  private ResourceBundle resourceBundle;

  private volatile List<Locale> availableLocales;

  private volatile EnvironmentVariables userEnvironmentVariables;

  private static final Pattern USER_LANG_PATTERN = Pattern.compile("-Duser\\.language=(\\S*)");

  /**
   * Creates the service and initializes it with the given locale.
   *
   * @param locale the preferred locale, or {@code null} to use persisted or default locale.
   */
  public NlsService(Locale locale) {

    initialize(locale);
  }

  /**
   * Initializes the service using an explicit, persisted, or system default locale.
   *
   * @param explicitLocale the locale requested by the caller, or {@code null}.
   */
  private void initialize(Locale explicitLocale) {

    Locale localeToApply = explicitLocale;
    if (localeToApply == null) {
      localeToApply = Locale.getDefault();
    }
    applyLocale(localeToApply, false);
  }

  /**
   * Changes the active locale and persists the selection.
   *
   * @param locale the new locale to activate.
   */
  public void setLocale(Locale locale) {

    applyLocale(locale, true);
  }

  /**
   * Applies the locale, reloads the bundle, optionally persists it, and notifies listeners.
   *
   * @param locale the locale to apply.
   * @param persist whether the locale should be stored in user configuration.
   */
  private void applyLocale(Locale locale, boolean persist) {

    Locale targetLocale = (locale == null) ? Locale.getDefault() : locale;
    boolean localeChanged = !targetLocale.equals(this.locale);
    if (localeChanged) {
      this.locale = targetLocale;
      loadBundle();
    }
    if (persist) {
      persistLocale(targetLocale);
    }
    if (!localeChanged) {
      return;
    }
    LOG.info("Locale set to: {}", targetLocale.toLanguageTag());
    for (Runnable listener : this.localeListeners) {
      try {
        listener.run();
      } catch (Exception e) {
        LOG.warn("Locale change listener threw: {}", e.getMessage(), e);
      }
    }
  }

  /**
   * Returns the locales supported by the GUI bundles.
   *
   * @return immutable list of supported locales.
   */
  public List<Locale> getAvailableLocales() {

    if (this.availableLocales == null) {
      synchronized (this) {
        if (this.availableLocales == null) {
          this.availableLocales = detectAvailableLocales();
        }
      }
    }
    return this.availableLocales;
  }

  /**
   * Returns the display label for a locale, using {@code CurrentLanguage} when available.
   *
   * @param locale the locale to describe.
   * @return a user-facing language label.
   */
  public String getLanguageDisplayName(Locale locale) {

    if (locale == null) {
      return "";
    }
    try {
      ResourceBundle bundle = loadBundle(locale);
      if (bundle.containsKey(LANGUAGE_DISPLAY_KEY)) {
        return bundle.getString(LANGUAGE_DISPLAY_KEY);
      }
    } catch (MissingResourceException e) {
      LOG.debug("No bundle found for language display locale {}", locale, e);
    }
    //fallback
    String languageCode = locale.getLanguage();
    if (languageCode.isBlank()) {
      languageCode = Locale.ENGLISH.getLanguage();
    }
    return locale.getDisplayLanguage(locale) + " (" + languageCode + ")";
  }

  /**
   * Registers a listener that is called after the locale changes.
   *
   * @param listener callback to run after locale updates.
   */
  public void addLocaleChangeListener(Runnable listener) {

    if (listener != null) {
      this.localeListeners.add(listener);
    }
  }

  /**
   * Removes a previously registered locale change listener.
   *
   * @param listener the callback to remove.
   */
  public void removeLocaleChangeListener(Runnable listener) {
    if (listener != null) {
      this.localeListeners.remove(listener);
    }
  }

  /**
   * Returns the active locale.
   *
   * @return the current locale.
   */
  public Locale getLocale() {

    return this.locale;
  }

  /**
   * Returns the current resource bundle.
   *
   * @return the active {@link ResourceBundle}.
   */
  public ResourceBundle getResourceBundle() {

    return this.resourceBundle;
  }

  /**
   * Looks up a translation key in the current bundle.
   *
   * @param key the message key.
   * @return the translated text or a fallback marker if missing.
   */
  public String get(String key) {

    return this.resourceBundle.getString(key);

  }

  /**
   * Loads the current bundle for the active locale, falling back to English if needed.
   */
  private void loadBundle() {

    try {
      this.resourceBundle = loadBundle(this.locale);
      LOG.debug("ResourceBundle loaded for locale: {}", this.locale);
    } catch (MissingResourceException e) {
      LOG.error("Failed to load ResourceBundle for locale: {}", this.locale, e);
      this.resourceBundle = loadBundle(Locale.ENGLISH);
    }
  }

  private ResourceBundle loadBundle(Locale locale) {

    return ResourceBundle.getBundle(BUNDLE_NAME, locale);
  }

  /**
   * Detects available locales by scanning the classpath for {@code nls/messages_*.properties} files, covering both exploded directories and JAR files.
   *
   * @return immutable list of supported locales.
   */
  private List<Locale> detectAvailableLocales() {

    Set<Locale> detectedLocales = new LinkedHashSet<>();
    detectedLocales.add(Locale.ENGLISH);

    String packagePath = BUNDLE_PACKAGE + "/";
    String filePrefix = BUNDLE_BASE_NAME + "_";

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = NlsService.class.getClassLoader();
    }

    try {
      Enumeration<URL> resources = loader.getResources(packagePath);
      while (resources.hasMoreElements()) {
        collectLocalesFromUrl(resources.nextElement(), packagePath, filePrefix, detectedLocales);
      }
    } catch (IOException e) {
      LOG.warn("Classpath locale discovery failed: {}", e.getMessage(), e);
    }

    return List.copyOf(detectedLocales);
  }

  private void collectLocalesFromUrl(URL url, String packagePath, String filePrefix, Set<Locale> locales) {

    if ("file".equals(url.getProtocol())) {
      try {
        collectLocalesFromDirectory(Path.of(url.toURI()), filePrefix, locales);
      } catch (Exception e) {
        LOG.debug("Locale scan skipped for {}: {}", url, e.getMessage(), e);
      }
    } else if ("jar".equals(url.getProtocol())) {
      collectLocalesFromJar(url, packagePath, filePrefix, locales);
    }
  }

  private void collectLocalesFromDirectory(Path dir, String filePrefix, Set<Locale> locales) {

    try (var stream = Files.list(dir)) {
      stream.map(p -> p.getFileName().toString())
          .filter(n -> n.startsWith(filePrefix) && n.endsWith(EXTENSION_PROPERTIES))
          .forEach(n -> addLocaleFromTag(n.substring(filePrefix.length(), n.length() - EXTENSION_PROPERTIES.length()), locales));
    } catch (IOException e) {
      LOG.debug("Could not list locale directory {}: {}", dir, e.getMessage(), e);
    }
  }

  private void collectLocalesFromJar(URL url, String packagePath, String filePrefix, Set<Locale> locales) {

    // url format: jar:file:/path/to/app.jar!/nls/
    String path = url.getPath();
    String jarFilePath = path.substring(0, path.indexOf('!'));
    String entryPrefix = packagePath + filePrefix;
    try {
      Path jar = Path.of(new URI(jarFilePath));
      try (JarFile jarFile = new JarFile(jar.toFile())) {
        jarFile.stream()
            .map(JarEntry::getName)
            .filter(n -> n.startsWith(entryPrefix) && n.endsWith(EXTENSION_PROPERTIES))
            .forEach(n -> addLocaleFromTag(n.substring(entryPrefix.length(), n.length() - EXTENSION_PROPERTIES.length()), locales));
      }
    } catch (Exception e) {
      LOG.debug("Could not scan JAR {} for locales: {}", url, e.getMessage(), e);
    }
  }

  private void addLocaleFromTag(String tag, Set<Locale> locales) {

    if (tag.isBlank()) {
      return;
    }
    // Java bundle filenames use '_' as separator (e.g. zh_CN); BCP 47 uses '-'
    Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
    if (!locale.getLanguage().isEmpty()) {
      locales.add(locale);
    }
  }


  /**
   * Persists the selected GUI locale by setting {@code -Duser.language=<tag>} inside {@code IDE_OPTIONS}. Updates an existing {@code -Duser.language=...} entry
   * if present; otherwise appends it.
   *
   * @param localeToPersist the locale to store.
   */
  private void persistLocale(Locale localeToPersist) {

    EnvironmentVariables environmentVariables = getUserEnvironmentVariables();
    try {
      String ideOptions = environmentVariables.get(IDE_OPTIONS);
      String userLangFlag = "-Duser.language=" + localeToPersist.toLanguageTag();
      String updated = updateLocale(ideOptions, userLangFlag);
      environmentVariables.set(IDE_OPTIONS, updated);
      environmentVariables.save();
    } catch (RuntimeException e) {
      LOG.warn("Failed to persist GUI locale: {}", e.getMessage(), e);
    }
  }

  String updateLocale(String ideOptions, String userLangFlag) {

    if (ideOptions == null || ideOptions.isBlank()) {
      return userLangFlag;
    }
    Matcher matcher = USER_LANG_PATTERN.matcher(ideOptions);
    if (matcher.find()) {
      return matcher.replaceFirst(userLangFlag);
    }
    return ideOptions + " " + userLangFlag;
  }

  private EnvironmentVariables getUserEnvironmentVariables() {

    if (this.userEnvironmentVariables == null) {
      synchronized (this) {
        if (this.userEnvironmentVariables == null) {
          this.userEnvironmentVariables = createUserEnvironmentVariables();
        }
      }
    }
    return this.userEnvironmentVariables;
  }

  private EnvironmentVariables createUserEnvironmentVariables() {

    IdeContext context = new IdeContextConsole();
    EnvironmentVariables userVariables = context.getVariables().getByType(EnvironmentVariablesType.USER);
    if (userVariables == null) {
      throw new IllegalStateException("Failed to resolve USER environment variables from IDE context");
    }
    return userVariables;
  }

}
