package com.anibalxyz.features.auth.api;

import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.common.Result;
import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.features.common.application.exception.FailureSignal;
import com.anibalxyz.server.config.modules.runtime.RuntimeConfig;
import com.anibalxyz.server.context.RequestContext;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import io.jsonwebtoken.Claims;
import java.util.Set;

public class JwtMiddleware implements RuntimeConfig {

  public static final String JWT_USER_ID = "jwt_userId";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";
  private final JwtService jwtService;

  public JwtMiddleware(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  // TODO: Missing branches will be covered soon with unit testing
  @Override
  public void apply(Javalin server) {
    server.beforeMatched(
        ctx -> {
          Set<RouteRole> permittedRoles = ctx.routeRoles();

          if (permittedRoles.isEmpty() || permittedRoles.contains(Role.GUEST)) {
            return;
          }

          // For any other role, run the JWT middleware to authenticate
          handle(ctx);

          // At this point, if jwtMiddleware didn't throw, the user is authenticated.
          // We can grant them the AUTHENTICATED role.
          Set<RouteRole> userRoles = Set.of(Role.AUTHENTICATED);

          if (userRoles.stream().noneMatch(permittedRoles::contains)) {
            throw new ForbiddenResponse("Access denied");
          }
        });
  }

  // TODO: rename more semantically
  public void handle(Context ctx) {
    String authHeader = ctx.header(AUTHORIZATION_HEADER);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      throw new UnauthorizedResponse("Missing or invalid Authorization header");
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    Result<Claims, JwtService.JwtValidationError> validationResult =
        jwtService.validateToken(token);

    if (validationResult.isFailure()) {
      throw new FailureSignal(validationResult.getError());
    }

    int userId = Integer.parseInt(validationResult.getValue().getSubject());
    RequestContext.setUserId(userId);
    ctx.attribute(JWT_USER_ID, userId);
  }
}
