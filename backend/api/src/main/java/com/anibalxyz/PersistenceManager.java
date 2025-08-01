package com.anibalxyz;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class PersistenceManager {
    private static EntityManagerFactory emf;

    public static void init() {
        if (emf != null && emf.isOpen()) {
            return;
        }

        // TODO: extract from here
        String dbPort = "5432";
        String dbHost = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        // TODO: move to a method
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", url);
        props.put("jakarta.persistence.jdbc.user", dbUser);
        props.put("jakarta.persistence.jdbc.password", dbPassword);

        props.put("hibernate.hbm2ddl.auto", "validate");

        emf = Persistence.createEntityManagerFactory("reconcilerPU", props);
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            init();
        }
        return emf;
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}