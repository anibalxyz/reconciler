package com.anibalxyz.system.api;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.system.api.out.HealthResponse;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;

public class SystemController implements SystemApi {

    private final PersistenceManager persistenceManager;

    public SystemController(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public void healthCheck(Context ctx) {
        boolean dbIsConnected;
        try (EntityManager em = persistenceManager.emf().createEntityManager()) {
            em.createNativeQuery("SELECT 1").getSingleResult();
            dbIsConnected = true;
        } catch (Exception e) {
            dbIsConnected = false;
        }
        ctx.json(new HealthResponse(dbIsConnected));
    }
}
