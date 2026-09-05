package com.anibalxyz.server.api;

import com.anibalxyz.core.api.exception.HttpException;
import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import io.javalin.router.EndpointNotFound;
import java.util.List;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

public class InfrastructureErrorMapper {

  private static final List<Resolver> resolvers =
      List.of(new Resolver.JsonError(), new Resolver.NotFound(), new Resolver.HttpError());

  private InfrastructureErrorMapper() {}

  public static ErrorResult map(Exception e) {
    return resolvers.stream()
        .map(resolver -> resolver.execute(e))
        .filter(errorResult -> errorResult.response() != null)
        .findFirst()
        .orElse(new ErrorResult(500, new ErrorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR)));
  }

  private sealed interface Resolver {
    ErrorResult execute(Exception e);

    final class JsonError implements Resolver {
      @Override
      public ErrorResult execute(Exception e) {
        ErrorResponse base = new ErrorResponse(CommonErrorCode.BAD_REQUEST);
        base =
            switch (e) {
              case UnrecognizedPropertyException ex ->
                  base.detail("Unrecognized property: '" + ex.getPropertyName() + "'");
              case StreamReadException ignored -> base.detail("Malformed JSON in request body");
              case MismatchedInputException ignored -> base.detail("Missing or empty request body");
              default -> null;
            };
        return new ErrorResult(400, base);
      }
    }

    final class NotFound implements Resolver {
      @Override
      public ErrorResult execute(Exception e) {
        if (!(e instanceof EndpointNotFound)) return new ErrorResult(404, null);
        return new ErrorResult(
            404, new ErrorResponse(CommonErrorCode.RESOURCE_NOT_FOUND).detail(e.getMessage()));
      }
    }

    final class HttpError implements Resolver {
      @Override
      public ErrorResult execute(Exception e) {
        if (!(e instanceof HttpException hex)) return new ErrorResult(500, null);
        return new ErrorResult(
            hex.status(), new ErrorResponse(hex.errorCode()).detail(hex.detail()));
      }
    }
  }
}
