package com.anibalxyz.system.api;

import com.anibalxyz.server.routes.RouteGroup;
import com.anibalxyz.server.routes.RouteRegistry;
import io.javalin.Javalin;

public class SystemRoutes extends RouteRegistry {

    private final SystemApi systemApi;

    public SystemRoutes(Javalin server, SystemApi systemApi) {
        super(server);
        this.systemApi = systemApi;
    }

    @Override
    public void register() {
        new RouteGroup("/health", server)
            .get("", systemApi::healthCheck);
    }
}
