package com.anibalxyz.server.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.anibalxyz.shared.IntegrationTest;
import io.javalin.Javalin;
import io.javalin.config.JavalinState;
import io.javalin.http.HandlerType;
import io.javalin.router.Endpoint;
import io.javalin.router.ParsedEndpoint;
import io.javalin.security.Roles;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts every route declared by the application has explicit roles. Plugin-owned paths are
 * allow-listed defensively; today Javalin does not enumerate them, so the list is inert.
 */
@DisplayName("Test that routes must declare explicit roles")
public class RoutesRolesIT extends IntegrationTest {

  private static final List<RouteExemption> EXEMPTED_ROUTES =
      List.of(
          new RouteExemption(null, "/swagger", "Swagger UI plugin"),
          new RouteExemption(null, "/openapi", "OpenAPI plugin documentation"),
          new RouteExemption(null, "/webjars/", "Swagger UI static assets"));

  private static boolean isExempted(HandlerType method, String path) {
    return EXEMPTED_ROUTES.stream()
        .anyMatch(
            exemption -> {
              boolean methodMatches =
                  exemption.method() == null || exemption.method().equals(method.name());
              boolean pathMatches =
                  exemption.path() == null
                      || path.equals(exemption.path())
                      || path.startsWith(exemption.path());
              return methodMatches && pathMatches;
            });
  }

  @Test
  @DisplayName("every registered route declares a non-empty routeRoles() set")
  public void eachRouteHasExplicitRoles() {
    Javalin j = app.javalin();
    JavalinState state = j.unsafe;
    List<ParsedEndpoint> handlers = state.internalRouter.allHttpHandlers();

    assertThat(handlers).as("application must register at least one route").isNotEmpty();

    for (ParsedEndpoint pe : handlers) {
      Endpoint ep = pe.endpoint;
      HandlerType method = ep.method;
      String path = ep.path;

      if (!method.isHttpMethod()) {
        continue;
      }

      Roles roles = ep.metadata(Roles.class);
      Set<?> effectiveRoles = roles == null ? Set.of() : roles.getRoles();

      if (isExempted(method, path)) {
        continue;
      }

      assertThat(effectiveRoles)
          .as(
              "route %s %s must declare at least one role in routeRoles() (plugin routes are"
                  + " exempted in this test, not in production code)",
              method, path)
          .isNotEmpty();
    }
  }

  private record RouteExemption(String method, String path, String reason) {}
}
