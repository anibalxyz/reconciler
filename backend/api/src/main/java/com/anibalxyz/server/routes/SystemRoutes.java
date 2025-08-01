package com.anibalxyz.server.routes;

import com.anibalxyz.PersistenceManager;
import io.javalin.Javalin;
import jakarta.persistence.EntityManager;

import java.util.Map;

public class SystemRoutes {
    private final Javalin app;

    public SystemRoutes(Javalin app) {
        this.app = app;
    }

    public void register() {
        this.app.get("/health", ctx -> {
            boolean dbIsConnected;

            try (EntityManager em = PersistenceManager.getEntityManagerFactory().createEntityManager()) {
                em.createNativeQuery("SELECT 1").getSingleResult();
                dbIsConnected = true;
            } catch (Exception e) {
                dbIsConnected = false;
            }

            ctx.json(Map.of("status", dbIsConnected));
        });
    }
}
