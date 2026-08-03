package com.devonfw.tools.ide.nls;

import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

/**
 * Wrapper for {@link ResourceBundle} to avoid {@link java.util.MissingResourceException}.
 */
public class NlsBundle {

  private static final Logger LOG = LoggerFactory.getLogger(NlsBundle.class);

  private final IdeContext context;

  private final String fqn;

  private final ResourceBundle bundle;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param name the simple name of {@link ResourceBundle} (e.g. "Cli").
   */
  public NlsBundle(IdeContext context, String name) {

    this(context, name, Locale.getDefault());
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param locale the explicit {@link Locale} to use.
   */
  public NlsBundle(IdeContext context, Locale locale) {

    this(context, "Help", locale);
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param name the simple name of {@link ResourceBundle} (e.g. "Cli").
   * @param locale the explicit {@link Locale} to use.
   */
  public NlsBundle(IdeContext context, String name, Locale locale) {

    super();
    this.context = context;
    this.fqn = "nls." + name;
    this.bundle = ResourceBundle.getBundle(this.fqn, locale);
  }

  /**
   * @param key the NLS key.
   * @return the localized message (translated to the users language).
   */
  public String get(String key) {

    if (!this.bundle.containsKey(key)) {
      LOG.warn("Cound not find key '{}' in ResourceBundle {}.properties", key, this.fqn);
      return "?" + key;
    }
    return this.bundle.getString(key);
  }

  /**
   * @param key the NLS key.
   * @return the localized message (translated to the users language) or {@code null} if undefined.
   */
  public String getOrNull(String key) {

    if (!this.bundle.containsKey(key)) {
      return null;
    }
    return this.bundle.getString(key);
  }

  /**
   * @param commandlet the {@link com.devonfw.tools.ide.commandlet.Commandlet} to get the help summary for.
   * @return the localized message (translated to the users language).
   */
  public String get(Commandlet commandlet) {

    return get("cmd." + commandlet.getName());
  }

  /**
   * @param commandlet the {@link com.devonfw.tools.ide.commandlet.Commandlet} to get the help detail for.
   * @return the localized message (translated to the users language).
   */
  public String getDetail(Commandlet commandlet) {

    return get("cmd." + commandlet.getName() + ".detail");
  }

  /**
   * @param commandlet the {@link Commandlet} {@link Commandlet#getProperties() owning} the given {@link Property}.
   * @param property the {@link Property} to the the description of.
   * @return the localized message describing the property.
   */
  public String get(Commandlet commandlet, Property<?> property) {

    String prefix = "opt.";
    if (property.isValue()) {
      prefix = "val.";
    }
    String key = prefix + property.getNameOrAlias();

    String qualifiedKey = "cmd." + commandlet.getName() + "." + key;
    String value = getOrNull(qualifiedKey);
    if (value == null) {
      value = getOrNull(key); // fallback to share messages across commandlets
      if (value == null) {
        value = get(key); // will fail to resolve but we want to reuse the code
      }
    }
    return value;
  }

  /**
   * @param context the {@link IdeContext}.
   * @return the {@link NlsBundle} for "Cli".
   */
  public static NlsBundle of(IdeContext context) {

    return new NlsBundle(context, context.getLocale());
  }

}
