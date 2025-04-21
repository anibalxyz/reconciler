package com.anibalxyz;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Enter to show db test data");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        String host = System.getenv("POSTGRES_HOST");
        String port = System.getenv("POSTGRES_PORT");
        String db = System.getenv("POSTGRES_DB");
        String user = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" +db;
        System.out.println("Conectando a la base de datos: " + url);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conexión establecida con éxito.");

            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM transactions";
            var rs = stmt.executeQuery(sql);

            while (rs.next()) {
                System.out.println("Resultado: " + rs.getString("description"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error al conectar a la base de datos.");
        }
    }
}
