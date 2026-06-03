package com.anibalxyz.server.config.modules.startup;

import com.anibalxyz.server.config.AppEnv;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.openapi.plugin.DefinitionConfiguration;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

/**
 * Configuration for OpenAPI documentation and Swagger UI integration.
 *
 * <p>This configuration sets up comprehensive API documentation including detailed project
 * information, contact details, license information, and server configurations. The OpenAPI
 * specification follows industry standards and provides complete API documentation.
 */
public class SwaggerConfig implements StartupConfig {

  private final ServerEnvironment env;

  public SwaggerConfig(ServerEnvironment env) {
    this.env = env;
  }

  public static void swaggerPatch(Context ctx, AppEnv env) {
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
  }

  public void registerSwaggerPlugin(JavalinConfig javalinConfig) {
    javalinConfig.registerPlugin(
        new SwaggerPlugin(
            swaggerConfig -> {
              swaggerConfig.setUiPath("/swagger");
            }));
  }

  private void registerOpenApiPlugin(JavalinConfig javalinConfig) {
    javalinConfig.registerPlugin(
        new OpenApiPlugin(
            openApiConfig ->
                openApiConfig
                    .withDocumentationPath("/openapi")
                    .withDefinitionConfiguration(this::definitionConfiguration)));
  }

  private void definitionConfiguration(String version, DefinitionConfiguration definition) {
    String infoDescription =
"""
Financial transaction reconciliation API for teams to reconcile transactions between bank statements and internal
systems. Built with clean architecture principles, domain-driven design, and comprehensive testing strategies.
""";
    definition
        .withInfo(
            info ->
                info.title("Reconciler API")
                    .version("0.0.0")
                    .description(infoDescription)
                    // .termsOfService
                    // ("https://github.com/anibalxyz/reconciler/blob/main/README.md")
                    .contact("Anibal Boggio", "https://github.com/anibalxyz", env.CONTACT_EMAIL())
                    .license(
                        "MIT License",
                        "https://github.com/anibalxyz/reconciler/blob/main/LICENSE",
                        "MIT"))
        .withSecurity(openApiSecurity -> openApiSecurity.withBearerAuth("bearerAuth"))
        .withDefinitionProcessor(this::definitionProcessor);
    setServers(definition);
  }

  private void setServers(DefinitionConfiguration definition) {
    if (env.APP_ENV() == AppEnv.PROD) {
      definition.withServer(
          server -> server.description("Production Server").url(env.API_PUBLIC_URL()));
    } else {
      definition
          .withServer(
              server ->
                  server
                      .description("API PREFIX only - proxied by frontend (the most comfortable)")
                      .url("/api"))
          .withServer(
              server ->
                  server
                      .description(
                          "API URL - direct-to-backend url but needs proper CORS configuration (/health does not work)")
                      .url(env.API_URL()))
          .withServer(
              server ->
                  server
                      .description(
                          "ROOT URL - currently used to complement API URL server (enables /health but blocks the rest)")
                      .url(env.SERVER_URL()));
    }
  }

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
  }
}
