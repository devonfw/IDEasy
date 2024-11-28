package com.devonfw.tools.ide.environment;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Implementation of {@link IdeSystem}.
 */
public class IdeSystemImpl implements IdeSystem {

  private final IdeLogger logger;

  final Properties systemProperties;

  final Map<String, String> environmentVariables;

  /**
   * @param logger the {@link IdeLogger}.
   */
  public IdeSystemImpl(IdeLogger logger) {

    this(logger, System.getProperties(), System.getenv());
  }

  /**
   * @param logger the {@link IdeLogger}.
   * @param systemProperties the {@link System#getProperties() system properties}.
   * @param environmentVariables the {@link System#getenv() environment variables}.
   */
  protected IdeSystemImpl(IdeLogger logger, Properties systemProperties, Map<String, String> environmentVariables) {

    super();
    this.logger = logger;
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
      this.logger.trace("System property was already set to {}={}", key, value);
    } else {
      this.systemProperties.put(key, value);
      if (old == null) {
        this.logger.trace("System property was set to {}={}", key, value);
      } else {
        this.logger.trace("System property was changed to {}={} from {}", key, value, old);
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
