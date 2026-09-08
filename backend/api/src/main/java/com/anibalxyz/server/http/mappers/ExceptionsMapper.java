package com.anibalxyz.server.http.mappers;

import com.anibalxyz.core.api.HttpException;
import com.anibalxyz.core.api.response.error.CommonErrorCode;
import com.anibalxyz.core.api.response.error.ErrorResponse;
import com.anibalxyz.core.api.response.mappers.ErrorMapperResult;
import io.javalin.router.EndpointNotFound;
import java.util.List;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

public class ExceptionsMapper {
  private static final int INVALID_RESOLVER = 12345;

  private static final List<Resolver> resolvers =
      List.of(new Resolver.JsonError(), new Resolver.NotFound(), new Resolver.HttpError());

  private ExceptionsMapper() {}

  public static ErrorMapperResult map(Exception e) {
    return resolvers.stream()
        .map(resolver -> resolver.execute(e))
        .filter(errorResult -> errorResult.response() != null)
        .findFirst()
        .orElse(
            new ErrorMapperResult(500, new ErrorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR)));
  }

  private sealed interface Resolver {
    ErrorMapperResult execute(Exception e);

    final class JsonError implements Resolver {
      @Override
      public ErrorMapperResult execute(Exception e) {
        ErrorResponse base = new ErrorResponse(CommonErrorCode.BAD_REQUEST);
        base =
            switch (e) {
              case UnrecognizedPropertyException ex ->
                  base.detail("Unrecognized property: '" + ex.getPropertyName() + "'");
              case StreamReadException ignored -> base.detail("Malformed JSON in request body");
              case MismatchedInputException ignored -> base.detail("Missing or empty request body");
              default -> null;
            };
        return new ErrorMapperResult(400, base);
      }
    }

    final class NotFound implements Resolver {
      @Override
      public ErrorMapperResult execute(Exception e) {
        if (!(e instanceof EndpointNotFound)) return new ErrorMapperResult(INVALID_RESOLVER, null);
        return new ErrorMapperResult(
            404, new ErrorResponse(CommonErrorCode.RESOURCE_NOT_FOUND).detail(e.getMessage()));
      }
    }

    final class HttpError implements Resolver {
      @Override
      public ErrorMapperResult execute(Exception e) {
        if (!(e instanceof HttpException hex)) return new ErrorMapperResult(INVALID_RESOLVER, null);
        return new ErrorMapperResult(
            hex.status(), new ErrorResponse(hex.errorCode()).detail(hex.detail()));
      }
    }
  }
}
