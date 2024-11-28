package com.devonfw.tools.ide.environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Extends {@link IdeSystemImpl} for testing. It will not modify your {@link System} and allows to modify environment variables for testing.
 */
public class IdeSystemTestImpl extends IdeSystemImpl {

  /**
   * @param logger the {@link IdeLogger}.
   */
  public IdeSystemTestImpl(IdeLogger logger) {

    this(logger, new Properties(), new HashMap<>());
    this.environmentVariables.put("PATH", System.getenv("PATH"));
  }

  /**
   * @param logger the {@link IdeLogger}.
   * @param systemProperties the {@link System#getProperties() system properties} for testing.
   * @param environmentVariables the {@link System#getenv() environment variables} for testing.
   */
  public IdeSystemTestImpl(IdeLogger logger, Properties systemProperties,
      Map<String, String> environmentVariables) {

    super(logger, systemProperties, environmentVariables);
  }

  /**
   * @param key the name of the environment variable to mock.
   * @param value the value of the environment variable to mock.
   */
  public void setEnv(String key, String value) {

    this.environmentVariables.put(key, value);
  }

  /**
   * @return the internal system {@link Properties}.
   */
  public Properties getProperties() {

    return this.systemProperties;
  }

  /**
   * @param logger the {@link IdeLogger}.
   * @return a new instance of {@link IdeSystemTestImpl} initialized with {@link System} values but decoupled so changes do not affect {@link System}.
   */
  public static IdeSystemTestImpl ofSystemDefaults(IdeLogger logger) {

    return new IdeSystemTestImpl(logger, new Properties(System.getProperties()), new HashMap<>(System.getenv()));
  }
}
