
package main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EmployeeDAO {

    // Add an employee
    public void addEmployee(String name, String department, double salary) {
        String sql = "INSERT INTO employees (name, department, salary) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, department);
                ps.setDouble(3, salary);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Employee added successfully!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // View all employees
    public void viewEmployees() {
        String sql = "SELECT * FROM employees";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                return;
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println("ID: " + rs.getInt("id"));
                    System.out.println("Name: " + rs.getString("name"));
                    System.out.println("Department: " + rs.getString("department"));
                    System.out.println("Salary: " + rs.getDouble("salary"));
                    System.out.println("-------------------------");
                }

                if (!found) {
                    System.out.println("No employees found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update an employee
    public void updateEmployee(int id, String name, String department, double salary) {
        String sql = "UPDATE employees SET name = ?, department = ?, salary = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, department);
                ps.setDouble(3, salary);
                ps.setInt(4, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Employee updated successfully!");
                } else {
                    System.out.println("Employee ID not found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete an employee
    public void deleteEmployee(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Employee deleted successfully!");
                } else {
                    System.out.println("Employee ID not found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
