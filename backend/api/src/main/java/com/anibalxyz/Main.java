package com.anibalxyz;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EnumSet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.FilterHolder;

public class Main {
    public static void main(String[] args) throws Exception {
        PersistenceManager.init();

        String dbPort = "5432";
        String dbHost = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        Server server = new Server(4000); // Docker internal port

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        FilterHolder cors = getFilterHolder();
        context.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);

                boolean isDbConnected;

                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    isDbConnected = conn != null && !conn.isClosed();
                } catch (SQLException e) {
                    isDbConnected = false;
                }

                String json = "{\"status\": " + isDbConnected + "}";
                resp.getWriter().write(json);
            }
        }), "/health");

        server.setHandler(context);
        server.start();
        server.join();

        PersistenceManager.shutdown();
    }

    private static FilterHolder getFilterHolder() {
        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
        // Only for testing purposes, should be restricted depending on the environment
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,OPTIONS");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Origin,Content-Type,Accept");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
        return cors;
    }
}
