
package main.java;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EmployeeServer {

    private static final Path WEB_ROOT =
            Paths.get("web").toAbsolutePath().normalize();

    public static void main(String[] args) throws IOException {

        HttpServer server =
                HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        server.createContext("/api/employees", EmployeeServer::handleApi);
        server.createContext("/", EmployeeServer::handleStatic);

        server.start();

        System.out.println("Employee Management Server started!");
        System.out.println("Open http://localhost:8080/dashboard.html");
    }

    private static void handleApi(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/employees") && method.equals("GET")) {
                viewEmployees(exchange);
                return;
            }

            if (method.equals("POST")) {
                Map<String, String> form = readForm(exchange);

                switch (path) {
                    case "/api/employees/add":
                        addEmployee(exchange, form);
                        return;

                    case "/api/employees/update":
                        updateEmployee(exchange, form);
                        return;

                    case "/api/employees/delete":
                        deleteEmployee(exchange, form);
                        return;

                    default:
                        sendJson(exchange, 404,
                                "{\"error\":\"API endpoint not found\"}");
                        return;
                }
            }

            sendJson(exchange, 405,
                    "{\"error\":\"Method not allowed\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500,
                    "{\"error\":\"Server error. Check the VS Code terminal.\"}");
        }
    }

    private static void viewEmployees(HttpExchange exchange)
            throws SQLException, IOException {

        StringBuilder json = new StringBuilder("{\"employees\":[");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                throw new SQLException("Database connection unavailable");
            }

            String sql =
                    "SELECT id, name, department, salary FROM employees";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                boolean first = true;

                while (rs.next()) {
                    if (!first) {
                        json.append(",");
                    }

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id"))
                            .append(",\"name\":\"")
                            .append(escapeJson(rs.getString("name")))
                            .append("\",\"department\":\"")
                            .append(escapeJson(rs.getString("department")))
                            .append("\",\"salary\":")
                            .append(rs.getDouble("salary"))
                            .append("}");

                    first = false;
                }
            }
        }

        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private static void addEmployee(
            HttpExchange exchange, Map<String, String> form)
            throws SQLException, IOException {

        String name = form.getOrDefault("name", "").trim();
        String department = form.getOrDefault("department", "").trim();
        double salary;

        if (name.isEmpty() || department.isEmpty()) {
            sendJson(exchange, 400,
                    "{\"error\":\"Name and department are required\"}");
            return;
        }

        try {
            salary = Double.parseDouble(form.getOrDefault("salary", ""));

            if (!Double.isFinite(salary) || salary < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendJson(exchange, 400,
                    "{\"error\":\"Enter a valid non-negative salary\"}");
            return;
        }

        String sql =
                "INSERT INTO employees (name, department, salary) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                throw new SQLException("Database connection unavailable");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, department);
                ps.setDouble(3, salary);
                ps.executeUpdate();
            }
        }

        sendJson(exchange, 201,
                "{\"message\":\"Employee added successfully\"}");
    }

    private static void updateEmployee(
            HttpExchange exchange, Map<String, String> form)
            throws SQLException, IOException {

        int id;
        double salary;

        try {
            id = Integer.parseInt(form.getOrDefault("id", ""));
            salary = Double.parseDouble(form.getOrDefault("salary", ""));

            if (id <= 0 || !Double.isFinite(salary) || salary < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendJson(exchange, 400,
                    "{\"error\":\"Enter a valid employee ID and salary\"}");
            return;
        }

        String name = form.getOrDefault("name", "").trim();
        String department = form.getOrDefault("department", "").trim();

        if (name.isEmpty() || department.isEmpty()) {
            sendJson(exchange, 400,
                    "{\"error\":\"Name and department are required\"}");
            return;
        }

        String sql =
                "UPDATE employees SET name = ?, department = ?, salary = ? WHERE id = ?";

        int rows;

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                throw new SQLException("Database connection unavailable");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, department);
                ps.setDouble(3, salary);
                ps.setInt(4, id);
                rows = ps.executeUpdate();
            }
        }

        if (rows > 0) {
            sendJson(exchange, 200,
                    "{\"message\":\"Employee updated successfully\"}");
        } else {
            sendJson(exchange, 404,
                    "{\"error\":\"Employee ID not found\"}");
        }
    }

    private static void deleteEmployee(
            HttpExchange exchange, Map<String, String> form)
            throws SQLException, IOException {

        int id;

        try {
            id = Integer.parseInt(form.getOrDefault("id", ""));

            if (id <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendJson(exchange, 400,
                    "{\"error\":\"Enter a valid employee ID\"}");
            return;
        }

        String sql = "DELETE FROM employees WHERE id = ?";
        int rows;

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                throw new SQLException("Database connection unavailable");
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                rows = ps.executeUpdate();
            }
        }

        if (rows > 0) {
            sendJson(exchange, 200,
                    "{\"message\":\"Employee deleted successfully\"}");
        } else {
            sendJson(exchange, 404,
                    "{\"error\":\"Employee ID not found\"}");
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        Map<String, String> form = new HashMap<>();

        for (String item : body.split("&")) {
            int index = item.indexOf("=");

            if (index > 0) {
                String key = URLDecoder.decode(
                        item.substring(0, index),
                        StandardCharsets.UTF_8);

                String value = URLDecoder.decode(
                        item.substring(index + 1),
                        StandardCharsets.UTF_8);

                form.put(key, value);
            }
        }

        return form;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendJson(
            HttpExchange exchange, int status, String message)
            throws IOException {

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void handleStatic(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equals("GET")) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        String urlPath = exchange.getRequestURI().getPath();

        if (urlPath.equals("/")) {
            urlPath = "/index.html";
        }

        Path file = WEB_ROOT.resolve(urlPath.substring(1)).normalize();

        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "Page not found");
            return;
        }

        String name = file.getFileName().toString();
        String contentType = "application/octet-stream";

        if (name.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (name.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (name.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        }

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendText(
            HttpExchange exchange, int status, String message)
            throws IOException {

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
