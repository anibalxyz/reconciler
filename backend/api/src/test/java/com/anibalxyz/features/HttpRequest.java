package com.anibalxyz.features;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpRequest {
  public ObjectMapper mapper;
  public OkHttpClient client;
  public String baseUrl;

  public HttpRequest(ObjectMapper mapper, OkHttpClient client, String baseUrl) {
    this.mapper = mapper;
    this.client = client;
    this.baseUrl = baseUrl;
  }

  public Response get(String path) {
    Request request = new Request.Builder().url(baseUrl + path).get().build();
    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Response post(String path, Object body) {
    try {
      String jsonBody =
          body.getClass().equals(String.class) ? (String) body : mapper.writeValueAsString(body);

      Request request =
          new Request.Builder()
              .url(baseUrl + path)
              .post(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json")))
              .build();

      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Response put(String path, Object body) {
    try {
      String jsonBody =
          body.getClass().equals(String.class) ? (String) body : mapper.writeValueAsString(body);

      Request request =
          new Request.Builder()
              .url(baseUrl + path)
              .put(okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json")))
              .build();

      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Response delete(String path) {
    Request request = new Request.Builder().url(baseUrl + path).delete().build();
    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T parseBody(Response response, TypeReference<T> typeRef) {
    try (ResponseBody body = response.body()) {
      assertNotNull(body);
      return mapper.readValue(body.string(), typeRef);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
