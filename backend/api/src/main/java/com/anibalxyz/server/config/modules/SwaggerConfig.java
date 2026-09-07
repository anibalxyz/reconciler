package com.anibalxyz.server.config.modules;

import com.anibalxyz.core.AppEnv;
import com.anibalxyz.features.common.api.Role;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.openapi.schema.OpenApiSchemaBuilder;

/**
 * Configuration for OpenAPI documentation and Swagger UI integration.
 *
 * <p>This configuration sets up comprehensive API documentation including detailed project
 * information, contact details, license information, and server configurations. The OpenAPI
 * specification follows industry standards and provides complete API documentation.
 */
public class SwaggerConfig implements StartupConfig {

  private final Config config;
  private final AppEnv appEnv;

  public SwaggerConfig(Config config, AppEnv appEnv) {
    this.config = config;
    this.appEnv = appEnv;
  }

  private static void swaggerPatch(Context ctx, AppEnv env) {
    String html = ctx.result();
    if (html == null) return;
    String credentialsOption = env == AppEnv.PROD ? "same-origin" : "include";
    String patch =
"""
<script>
  (function() {
    const originalFetch = window.fetch;
    window.fetch = function(...args) {
      const options = args[1] || {};
      options.credentials = '%s';
      args[1] = options;
      return originalFetch.apply(this, args);
    };
    console.info("Swagger patched Successfully via 'after' handler");
  })();
</script>
"""
            .formatted(credentialsOption);
    ctx.result(html.replace("</body>", patch + "</body>"));
  }

  @Override
  public void apply(JavalinConfig javalinConfig) {
    registerOpenApiPlugin(javalinConfig);
    registerSwaggerPlugin(javalinConfig);
    javalinConfig.routes.after("/swagger", ctx -> swaggerPatch(ctx, appEnv));
  }

  public void registerSwaggerPlugin(JavalinConfig javalinConfig) {
    javalinConfig.registerPlugin(
        new SwaggerPlugin(
            swaggerConfig -> swaggerConfig.withUiPath("/swagger").withRoles(Role.GUEST)));
  }

  private void registerOpenApiPlugin(JavalinConfig javalinConfig) {
    javalinConfig.registerPlugin(
        new OpenApiPlugin(
            openApiConfig ->
                openApiConfig
                    .withDocumentationPath("/openapi")
                    .withRoles(Role.GUEST)
                    .withDefinitionConfiguration(this::definitionConfiguration)
            //                    .withDefinitionProcessor(this::definitionProcessor)
            ));
  }

  private void definitionConfiguration(String version, OpenApiSchemaBuilder definition) {
    String infoDescription =
"""
Financial transaction reconciliation API for teams to reconcile transactions between bank statements and internal
systems. Built with clean architecture principles, domain-driven design, and comprehensive testing strategies.
""";
    definition
        .info(
            info ->
                info.title("Reconciler API")
                    .version("0.0.0")
                    .description(infoDescription)
                    // .termsOfService
                    // ("https://github.com/anibalxyz/reconciler/blob/main/README.md")
                    .contact("Anibal Boggio", "https://github.com/anibalxyz", config.contactEmail())
                    .license(
                        "MIT License",
                        "https://github.com/anibalxyz/reconciler/blob/main/LICENSE",
                        "MIT"))
        .withBearerAuth("bearerAuth");

    setServers(definition);
  }

  private void setServers(OpenApiSchemaBuilder definition) {
    if (appEnv == AppEnv.PROD) {
      definition.server(
          openApiServer ->
              openApiServer.description("Production Server").url(config.apiPublicUrl()));
    } else {
      definition
          .server(
              server ->
                  server
                      .description("API PREFIX only - proxied by frontend (the most comfortable)")
                      .url("/api"))
          .server(
              server ->
                  server
                      .description(
                          "API URL - direct-to-backend url but needs proper CORS configuration (/health does not work)")
                      .url(config.apiUrl()))
          .server(
              server ->
                  server
                      .description(
                          "ROOT URL - currently used to complement API URL server (enables /health but blocks the rest)")
                      .url(config.serverUrl()));
    }
  }

  /*
  // TODO: uncomment once Javalin OpenAPI plugin uses jackson v3
  private String definitionProcessor(ObjectNode content) {
    ObjectNode externalDocs = content.objectNode();
    externalDocs.set("description", new TextNode("Project Repository and Documentation"));
    externalDocs.set("url", new TextNode("https://github.com/anibalxyz/reconciler"));
    content.set("externalDocs", externalDocs);

    // Add global tags for the API organization
    var tagsArray = content.arrayNode();

    var usersTag = content.objectNode();
    usersTag.set("name", new TextNode("Users"));
    usersTag.set(
        "description",
        new TextNode(
            "User management operations including CRUD functionality, authentication, and authorization features."));
    tagsArray.add(usersTag);

    var authTag = content.objectNode();
    authTag.set("name", new TextNode("Authentication"));
    authTag.set(
        "description",
        new TextNode("Endpoints for user authentication, including login and logout."));
    tagsArray.add(authTag);

    var systemTag = content.objectNode();
    systemTag.set("name", new TextNode("System"));
    systemTag.set(
        "description",
        new TextNode("System-level operations, such as health checks and status monitoring."));
    tagsArray.add(systemTag);

    content.set("tags", tagsArray);

    return content.toPrettyString();
  } */

  public interface Config {
    String apiUrl();

    String serverUrl();

    String contactEmail();

    String apiPublicUrl();
  }
}
