package com.devonfw.tools.ide.network;

import java.net.UnknownHostException;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock implementation of {@link NetworkStatusImpl} for testing.
 */
public class NetworkStatusMock extends NetworkStatusImpl {

  /**
   * @param context the {@link AbstractIdeTestContext}.
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo}. May be {@code null}.
   */
  public NetworkStatusMock(AbstractIdeTestContext context, WireMockRuntimeInfo wireMockRuntimeInfo) {
    super(context, statusUrl(wireMockRuntimeInfo), Long.MAX_VALUE);
  }

  private static String statusUrl(WireMockRuntimeInfo wireMockRuntimeInfo) {
    if (wireMockRuntimeInfo == null) {
      return null;
    }
    return wireMockRuntimeInfo.getHttpBaseUrl() + "/health";
  }

  /**
   * @return the exposed internal {@link CachedValue} for mocking during tests.
   */
  public CachedValue<Throwable> getOnlineCheck() {
    return this.onlineCheck;
  }

  /**
   * Simulates a network error indicating you are offline.
   *
   * @return the simulated {@link Exception}.
   */
  public Exception simulateNetworkError() {
    UnknownHostException error = new UnknownHostException("www.github.com");
    this.onlineCheck.set(error);
    return error;
  }

  /**
   * Simulates that we are online without any network access.
   */
  public void simulateOnline() {
    this.onlineCheck.set(null);
  }

}
