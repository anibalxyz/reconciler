package com.anibalxyz.server.api;

import com.anibalxyz.features.common.api.out.code.CommonErrorCode;
import com.anibalxyz.features.common.api.out.response.error.ErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.router.EndpointNotFound;
import java.util.List;

// TODO: implement exception handling for io.jsonwebtoken
// TODO: implement mapper for UnhandledErrorException and UnreachableCodeException
// TODO: implement unit-testing (currently almost full covered thanks to E2E)
public class InfrastructureErrorMapper {

  private static final List<Resolver> resolvers =
      List.of(new Resolver.BadRequest(), new Resolver.Auth(), new Resolver.NotFound());

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

    final class BadRequest implements Resolver {
      @Override
      public ErrorResult execute(Exception e) {
        ErrorResponse base = new ErrorResponse(CommonErrorCode.BAD_REQUEST);
        base =
            switch (e) {
              case UnrecognizedPropertyException ex ->
                  base.detail("Unrecognized property: '" + ex.getPropertyName() + "'");
              case JsonParseException ignored -> base.detail("Malformed JSON in request body");
              case MismatchedInputException ignored -> base.detail("Missing or empty request body");
              case BadRequestResponse ex -> base.detail(ex.getMessage());
              default -> null;
            };
        return new ErrorResult(400, base);
      }
    }

    final class Auth implements Resolver {
      @Override
      public ErrorResult execute(Exception e) {
        ErrorResponse base = new ErrorResponse(CommonErrorCode.UNAUTHORIZED);
        base =
            switch (e) {
              case UnauthorizedResponse ur -> base.detail(ur.getMessage());
              case ForbiddenResponse fr -> base.detail(fr.getMessage());
              default -> null;
            };
        return new ErrorResult(401, base);
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
  }
}
