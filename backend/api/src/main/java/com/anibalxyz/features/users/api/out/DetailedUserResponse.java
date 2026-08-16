package com.anibalxyz.features.users.api.out;

import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.common.api.out.response.success.ResponseMeta;
import com.anibalxyz.features.common.api.out.response.success.SuccessResponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiName;
import java.time.Instant;
import java.util.List;

public record DetailedUserResponse(
    @OpenApiExample("1") int id,
    @OpenApiExample("John Doe") String name,
    @OpenApiExample("john.doe@example.com") String email,
    @OpenApiExample("2025-10-10T10:00:00") Instant createdAt,
    @OpenApiExample("2025-10-10T10:00:00") Instant updatedAt)
    implements SuccessResponse {

  @JsonPropertyOrder({"data", "meta"})
  @OpenApiName("UserDetailResponseCollection")
  public static class Collection extends CollectionResponse<DetailedUserResponse> {
    public Collection(List<DetailedUserResponse> data, ResponseMeta meta) {
      super(data, meta);
    }

    @Override
    @OpenApiName("data")
    public List<DetailedUserResponse> data() {
      return super.data();
    }

    @Override
    @OpenApiName("meta")
    public ResponseMeta meta() {
      return super.meta();
    }
  }
}
