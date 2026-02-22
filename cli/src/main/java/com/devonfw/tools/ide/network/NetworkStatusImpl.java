package com.devonfw.tools.ide.network;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Implementation of {@link NetworkStatus}.
 */
public class NetworkStatusImpl implements NetworkStatus {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkStatusImpl.class);

  private final AbstractIdeContext context;

  private NetworkProxy networkProxy;

  private final String onlineCheckUrl;

  protected final CachedValue<Throwable> onlineCheck;

  /**
   * @param ideContext the {@link AbstractIdeContext}.
   */
  public NetworkStatusImpl(AbstractIdeContext ideContext) {
    this(ideContext, null, CachedValue.DEFAULT_RETENTION);
  }

  /**
   * @param context the {@link AbstractIdeContext}.
   * @param onlineCheckUrl the URL to test for the online-check.
   * @param retention the retention of the {@link CachedValue}.
   */
  protected NetworkStatusImpl(AbstractIdeContext context, String onlineCheckUrl, long retention) {
    this.context = context;
    if (onlineCheckUrl == null) {
      onlineCheckUrl = "https://www.github.com";
    }
    this.onlineCheckUrl = onlineCheckUrl;
    this.onlineCheck = new CachedValue<>(this::doOnlineCheck, retention);
  }

  @Override
  public boolean isOfflineMode() {

    return this.context.isOfflineMode();
  }

  @Override
  public boolean isOnline() {

    return getError() == null;
  }

  @Override
  public Throwable getError() {

    return this.onlineCheck.get();
  }

  private Throwable doOnlineCheck() {
    configureNetworkProxy();
    try {
      int timeout = 1000;
      //open a connection to URL and try to retrieve data
      //getContent fails if there is no connection
      URLConnection connection = new URL(this.onlineCheckUrl).openConnection();
      connection.setConnectTimeout(timeout);
      connection.getContent();
      return null;
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error when trying to connect to {}", this.onlineCheckUrl, e);
      }
      return e;
    }
  }

  private void configureNetworkProxy() {

    if (this.networkProxy == null) {
      this.networkProxy = new NetworkProxy(this.context);
      this.networkProxy.configure();
    }
  }

  @Override
  public void logStatusMessage() {

    if (isOfflineMode()) {
      LOG.warn("You are offline because you have enabled offline mode via CLI option.");
      return;
    }
    Throwable error = getError();
    if (error == null) {
      LOG.info(IdeLogLevel.INTERACTION.getSlf4jMarker(), "You are online.");
      return;
    }
    String message = "You are offline because of the following error:";
    if (LOG.isDebugEnabled()) {
      LOG.error(message, error);
    } else {
      LOG.error(message);
      LOG.error(error.toString());
    }
    if (error instanceof SSLException) {
      LOG.warn(
          "You are having TLS issues. We guess you are forced to use a VPN tool breaking end-to-end encryption causing this effect. As a workaround you can create and configure a truststore as described here:");
      LOG.info(IdeLogLevel.INTERACTION.getSlf4jMarker(), "https://github.com/devonfw/IDEasy/blob/main/documentation/proxy-support.adoc#tls-certificate-issues");
    } else {
      LOG.info(IdeLogLevel.INTERACTION.getSlf4jMarker(),
          "Please check potential proxy settings, ensure you are properly connected to the internet and retry this operation.");
    }
  }

  @Override
  public <T> T invokeNetworkTask(Callable<T> callable, String uri) {

    if (isOfflineMode()) {
      throw CliOfflineException.ofDownloadViaUrl(uri);
    }
    configureNetworkProxy();
    try {
      return callable.call();
    } catch (IOException e) {
      this.onlineCheck.set(e);
      throw new IllegalStateException("Network error whilst communicating to " + uri, e);
    } catch (Exception e) {
      throw new IllegalStateException("Unexpected checked exception whilst communicating to " + uri, e);
    }
  }
}
