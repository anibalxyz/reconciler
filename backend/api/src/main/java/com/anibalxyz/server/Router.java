package com.anibalxyz.server;

import com.anibalxyz.server.routes.SystemRoutes;
import com.anibalxyz.server.routes.UserRoutes;
import io.javalin.Javalin;

public class Router {
    public static void init(Javalin app, int port) {
        new UserRoutes(app).register();
        new SystemRoutes(app).register();
        app.start(port);
    }
}
