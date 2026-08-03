package com.devonfw.tools.ide.environment;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link IdeSystem}.
 */
public class IdeSystemImpl implements IdeSystem {

  private static final Logger LOG = LoggerFactory.getLogger(IdeSystemImpl.class);

  final Properties systemProperties;

  final Map<String, String> environmentVariables;

  /**
   * The constructor.
   */
  public IdeSystemImpl() {

    this(System.getProperties(), System.getenv());
  }

  /**
   * The constructor.
   *
   * @param systemProperties the {@link System#getProperties() system properties}.
   * @param environmentVariables the {@link System#getenv() environment variables}.
   */
  protected IdeSystemImpl(Properties systemProperties, Map<String, String> environmentVariables) {

    super();
    this.systemProperties = systemProperties;
    this.environmentVariables = environmentVariables;
  }

  @Override
  public String getProperty(String key) {

    return this.systemProperties.getProperty(key);
  }

  @Override
  public String getProperty(String key, String fallback) {

    return this.systemProperties.getProperty(key, fallback);
  }

  @Override
  public void setProperty(String key, String value) {

    String old = getProperty(key);
    if (Objects.equals(old, value)) {
      LOG.trace("System property was already set to {}={}", key, value);
    } else {
      this.systemProperties.put(key, value);
      if (old == null) {
        LOG.trace("System property was set to {}={}", key, value);
      } else {
        LOG.trace("System property was changed to {}={} from {}", key, value, old);
      }
    }
  }

  @Override
  public String getEnv(String key) {

    return this.environmentVariables.get(key);
  }

  @Override
  public Map<String, String> getEnv() {

    return this.environmentVariables;
  }
}
