package com.devonfw.tools.ide.io;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

/**
 * Base class providing support for HTTP downloads.
 */
public abstract class HttpDownloader {

  /** @see Builder#GET() */
  protected static final String HTTP_METHOD_GET = "GET";

  /** @see Builder#POST(BodyPublisher) */
  protected static final String HTTP_METHOD_POST = "POST";

  /** @see Builder#PUT(BodyPublisher) */
  protected static final String HTTP_METHOD_PUT = "PUT";

  /** @see Builder#DELETE() */
  protected static final String HTTP_METHOD_DELETE = "DELETE";

  /** @see Builder#HEAD() */
  protected static final String HTTP_METHOD_HEAD = "HEAD";

  /** HTTP method: {@value} */
  protected static final String HTTP_METHOD_OPTIONS = "OPTIONS";

  /** HTTP method: {@value} */
  protected static final String HTTP_METHOD_TRACE = "TRACE";

  /** HTTP method: {@value} */
  protected static final String HTTP_METHOD_PATCH = "PATCH";

  protected static HttpClient createHttpClient() {

    return HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
  }

  /**
   * Retrieves the {@link HttpResponse} body from a given URL as {@link String}.
   *
   * @param url the URL to retrieve the response body from.
   * @return a string representing the response body.
   * @throws IllegalStateException if the response body could not be retrieved.
   */
  protected static String httpGetAsString(String url) {

    try (HttpClient client = createHttpClient()) {
      HttpRequest request = createGetRequest(url);
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return response.body();
      }
      throw new IllegalStateException("Unexpected response code " + response.statusCode() + ":" + response.body());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve response body from url: " + url, e);
    }
  }

  /**
   * Retrieves the {@link HttpResponse} body as {@link InputStream} and processes it by the given {@link Consumer}.
   *
   * @param url the URL to retrieve the response body from.
   * @param httpVersion the HTTP {@link Version} to use.
   * @param bodyConsumer the {@link Consumer} to process the response body as {@link
   * @throws IllegalStateException if the response body could not be retrieved.
   */
  protected static void httpGet(String url, Version httpVersion, Consumer<HttpResponse<InputStream>> bodyConsumer) {

    try (HttpClient client = createHttpClient()) {
      HttpRequest request = createGetRequest(url, httpVersion);
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      int statusCode = response.statusCode();
      if (statusCode == 200) {
        bodyConsumer.accept(response);
      } else {
        throw new IllegalStateException("Download failed with status code " + statusCode);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to stream response body from url: " + url, e);
    }
  }

  /**
   * Creates a {@link #HTTP_METHOD_GET GET} {@link HttpRequest request}.
   *
   * @param url the URL.
   * @return the {@link HttpRequest}.
   */
  protected static HttpRequest createGetRequest(String url) {

    return createRequest(url, null, HTTP_METHOD_GET, null);
  }

  /**
   * Creates a {@link #HTTP_METHOD_GET GET} {@link HttpRequest request}.
   *
   * @param url the URL.
   * @param httpVersion the HTTP {@link Version}.
   * @return the {@link HttpRequest}.
   */
  protected static HttpRequest createGetRequest(String url, Version httpVersion) {

    return createRequest(url, httpVersion, HTTP_METHOD_GET, null);
  }

  /**
   * Creates a HTTP {@link HttpRequest request}.
   *
   * @param url the URL.
   * @param method the HTTP method. Please only provide constants such as {@link #HTTP_METHOD_DELETE}.
   * @param httpVersion the HTTP {@link Version}.
   * @return the {@link HttpRequest}.
   */
  protected static HttpRequest createRequest(String url, Version httpVersion, String method, BodyPublisher bodyPublisher) {

    Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url));
    builder = switch (method) {
      case HTTP_METHOD_GET -> builder.GET();
      case HTTP_METHOD_DELETE -> builder.DELETE();
      case HTTP_METHOD_HEAD -> builder.HEAD();
      default -> builder.method(method, bodyPublisher);
    };
    if (httpVersion != null) {
      builder.version(httpVersion);
    }
    return builder.build();
  }

}
