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
        String dbPort = System.getenv("POSTGRES_PORT");
        String dbHost = System.getenv("POSTGRES_HOST");
        String dbName = System.getenv("POSTGRES_DB");
        String dbUser = System.getenv("POSTGRES_USER");
        String dbPassword = System.getenv("POSTGRES_PASSWORD");
        String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        int apiPort = System.getenv("API_PORT") != null ? Integer.parseInt(System.getenv("API_PORT")) : 8080;
        Server server = new Server(apiPort);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
        // Only for testing purposes, should be restricted depending on the environment
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,OPTIONS");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "Origin,Content-Type,Accept");
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
        context.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);

                boolean isDbConnected = false;

                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    isDbConnected = conn != null && !conn.isClosed();
                } catch (SQLException e) {
                    isDbConnected = false;
                }

                String json = "{\"status\": " + isDbConnected + "}";
                resp.getWriter().write(json);
            }
        }), "/api/health");

        server.setHandler(context);
        server.start();
        server.join();
    }
}
