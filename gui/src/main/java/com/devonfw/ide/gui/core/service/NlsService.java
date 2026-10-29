package com.devonfw.ide.gui.core.service;

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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
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

  public static final String EXTENSION_PROPERTIES = ".properties";

  /**
   * Name of the user variable the GUI writes the selected language to and the CLI reads to launch the GUI with the corresponding locale.
   */
  public static final String GUI_LANGUAGE = "GUI_LANGUAGE";

  private Locale locale;

  private ResourceBundle resourceBundle;

  private volatile List<Locale> availableLocales;

  private volatile EnvironmentVariables userEnvironmentVariables;

  /** The {@link IdeContext} used to resolve the user environment variables. */
  private final IdeContext context;

  /**
   * Creates the service for the given locale.
   * <p>
   * The active locale is fixed for the lifetime of the service: the GUI JVM is launched by {@code ide gui} with the persisted language already applied
   * (via {@code -Duser.language}), so {@code Locale.getDefault()} is the applied locale. Changing the language is a persistence-only operation that takes
   * effect after a restart.
   * </p>
   * <p>
   * The {@link IdeContext} is always injected by the caller (see the {@code documentation/contributing/junit-testing.adoc} guidance): production code
   * passes an application-level context that resolves the real user configuration, while tests pass an isolated {@code IdeTestContext} to stay independent
   * from the machine's user home and the {@code GUI_LANGUAGE} user variable.
   * </p>
   *
   * @param context the {@link IdeContext} to use for resolving the user environment variables.
   * @param locale the locale to use, or {@code null} to use the default locale.
   */
  public NlsService(IdeContext context, Locale locale) {

    this.context = Objects.requireNonNull(context, "context must not be null");
    Locale targetLocale = (locale == null) ? Locale.getDefault() : locale;
    this.locale = targetLocale;
    loadBundle();
    LOG.info("Locale set to: {}", targetLocale.toLanguageTag());
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
   * Returns the display label for a locale localized using {@code this.locale}.
   *
   * @param locale the locale to describe.
   * @return a user-facing language label.
   */
  public String getLanguageDisplayName(Locale locale) {
    String languageCode = locale.getLanguage();
    if (languageCode.isBlank()) {
      languageCode = Locale.ENGLISH.getLanguage();
    }
    return locale.getDisplayLanguage(this.locale) + " (" + languageCode + ")";
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
   * Persists the selected GUI language as the language tag of the given locale by writing the {@code GUI_LANGUAGE} user variable. The selection takes effect
   * on the next GUI start, as {@code ide gui} reads this variable to apply the locale to the launched JVM.
   *
   * @param languageTag the BCP 47 language tag to persist (e.g. {@code de}, {@code en}).
   */
  public void setPersistedLocale(String languageTag) {

    EnvironmentVariables environmentVariables = getUserEnvironmentVariables();
    try {
      environmentVariables.set(GUI_LANGUAGE, languageTag);
      environmentVariables.save();
      LOG.info("Persisted GUI language: {}", languageTag);
    } catch (RuntimeException e) {
      LOG.warn("Failed to persist GUI language: {}", e.getMessage(), e);
    }
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

    EnvironmentVariables userVariables = this.context.getVariables().getByType(EnvironmentVariablesType.USER);
    if (userVariables == null) {
      throw new IllegalStateException("Failed to resolve USER environment variables from IDE context");
    }
    return userVariables;
  }

}
