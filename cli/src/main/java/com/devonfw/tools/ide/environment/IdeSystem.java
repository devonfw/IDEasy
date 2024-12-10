package com.devonfw.tools.ide.environment;

import java.util.Map;

/**
 * Interface to abstract from {@link System}.
 */
public interface IdeSystem {

  /**
   * @param key the name of the requested system property.
   * @return the {@link System#getProperty(String) value} of the requested system property.
   * @see System#getProperty(String)
   */
  String getProperty(String key);

  /**
   * @param key the name of the requested system property.
   * @param fallback the value to return as default in case the requested system property is undefined.
   * @return the {@link System#getProperty(String, String) value} of the requested system property.
   * @see System#getProperty(String, String)
   */
  String getProperty(String key, String fallback);

  /**
   * @param key the name of the system property to set.
   * @param value the new value to {@link System#setProperty(String, String) set}.
   * @see System#setProperty(String, String)
   */
  void setProperty(String key, String value);

  /**
   * @param key the name of the requested environment variable.
   * @return the {@link System#getenv(String) value} of the requested environment variable.
   * @see System#getenv(String)
   */
  String getEnv(String key);

  /**
   * @return the {@link System#getenv() environment variables}.
   */
  Map<String, String> getEnv();

}
