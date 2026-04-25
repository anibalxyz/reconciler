package com.anibalxyz.shared;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for simplifying the process of sending HTTP requests and parsing responses,
 * providing a convenient wrapper around OkHttp.
 */
public class HttpRequest {
  private final ObjectMapper mapper;
  private final OkHttpClient client;
  private final String baseUrl;

  public HttpRequest(ObjectMapper mapper, OkHttpClient client, String baseUrl) {
    this.mapper = mapper;
    this.client = client;
    this.baseUrl = baseUrl;
  }

  private Response executeRequest(Request request) {
    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Response get(String path) {
    return get(path, Map.of());
  }

  public Response get(String path, Map<String, String> headers) {
    Request.Builder requestBuilder = new Request.Builder().url(baseUrl + path).get();
    headers.forEach(requestBuilder::addHeader);
    return executeRequest(requestBuilder.build());
  }

  private RequestBody createJsonRequestBody(Object body) {
    try {
      String jsonBody =
          body.getClass().equals(String.class) ? (String) body : mapper.writeValueAsString(body);
      return okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Response post(String path, @NotNull Object body) {
    return post(path, body, Map.of());
  }

  public Response post(String path, @NotNull Object body, @NotNull Map<String, String> headers) {
    Request.Builder requestBuilder =
        new Request.Builder().url(baseUrl + path).post(createJsonRequestBody(body));
    headers.forEach(requestBuilder::addHeader);
    return executeRequest(requestBuilder.build());
  }

  public Response put(String path, Object body) {
    Request request =
        new Request.Builder().url(baseUrl + path).put(createJsonRequestBody(body)).build();
    return executeRequest(request);
  }

  public Response delete(String path) {
    Request request = new Request.Builder().url(baseUrl + path).delete().build();
    return executeRequest(request);
  }

  public <T> T parseBody(Response response, TypeReference<T> typeRef) {
    try (ResponseBody body = response.body()) {
      assertNotNull(body);
      return mapper.readValue(body.string(), typeRef);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T parseBody(Response response, Class<T> clazz) {
    try (ResponseBody body = response.body()) {
      assertNotNull(body);
      return mapper.readValue(body.string(), clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
