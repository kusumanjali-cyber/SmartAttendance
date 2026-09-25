import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/AdminStudentsServlet")
public class AdminStudentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/smartattendance"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        StringBuilder json = new StringBuilder();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String sql =
                    "SELECT id, student_name, roll_number, email, department, year " +
                    "FROM students ORDER BY id";

            try (Connection con =
                         DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                json.append("{\"success\":true,\"students\":[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"studentName\":\"")
                            .append(escapeJson(rs.getString("student_name")))
                            .append("\",")
                            .append("\"rollNumber\":\"")
                            .append(escapeJson(rs.getString("roll_number")))
                            .append("\",")
                            .append("\"email\":\"")
                            .append(escapeJson(rs.getString("email")))
                            .append("\",")
                            .append("\"department\":\"")
                            .append(escapeJson(rs.getString("department")))
                            .append("\",")
                            .append("\"year\":")
                            .append(rs.getInt("year"))
                            .append("}");
                }

                json.append("]}");
            }

            out.print(json.toString());

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                    "{\"success\":false,\"message\":\""
                    + escapeJson(e.getMessage())
                    + "\"}"
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        if (isEmpty(action)) {
            out.print(
                    "{\"success\":false,\"message\":\"Action is required\"}"
            );
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            if ("add".equalsIgnoreCase(action)) {

                addStudent(request, out);

            } else if ("update".equalsIgnoreCase(action)) {

                updateStudent(request, out);

            } else if ("delete".equalsIgnoreCase(action)) {

                deleteStudent(request, out);

            } else {

                out.print(
                        "{\"success\":false,\"message\":\"Invalid action\"}"
                );
            }

        } catch (NumberFormatException e) {

            out.print(
                    "{\"success\":false,\"message\":\"Year or ID must be a valid number\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                    "{\"success\":false,\"message\":\""
                    + escapeJson(e.getMessage())
                    + "\"}"
            );
        }
    }

    private void addStudent(HttpServletRequest request,
                            PrintWriter out)
            throws Exception {

        String name = request.getParameter("studentName");
        String roll = request.getParameter("rollNumber");
        String email = request.getParameter("email");
        String department = request.getParameter("department");
        String yearText = request.getParameter("year");

        if (isEmpty(name)
                || isEmpty(roll)
                || isEmpty(email)
                || isEmpty(department)
                || isEmpty(yearText)) {

            out.print(
                    "{\"success\":false,\"message\":\"Please fill all fields\"}"
            );
            return;
        }

        int year = Integer.parseInt(yearText);

        if (year < 1 || year > 10) {

            out.print(
                    "{\"success\":false,\"message\":\"Please enter a valid year\"}"
            );
            return;
        }

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String check =
                    "SELECT id FROM students " +
                    "WHERE roll_number = ? OR LOWER(email) = LOWER(?)";

            try (PreparedStatement ps = con.prepareStatement(check)) {

                ps.setString(1, roll.trim());
                ps.setString(2, email.trim());

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        out.print(
                                "{\"success\":false,\"message\":\"Roll number or email already exists\"}"
                        );
                        return;
                    }
                }
            }

            String sql =
                    "INSERT INTO students " +
                    "(student_name, roll_number, email, department, year) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name.trim());
                ps.setString(2, roll.trim());
                ps.setString(3, email.trim());
                ps.setString(4, department.trim());
                ps.setInt(5, year);

                ps.executeUpdate();

                out.print(
                        "{\"success\":true,\"message\":\"Student added successfully\"}"
                );
            }
        }
    }

    private void updateStudent(HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        String idText = request.getParameter("id");
        String name = request.getParameter("studentName");
        String roll = request.getParameter("rollNumber");
        String email = request.getParameter("email");
        String department = request.getParameter("department");
        String yearText = request.getParameter("year");

        if (isEmpty(idText)
                || isEmpty(name)
                || isEmpty(roll)
                || isEmpty(email)
                || isEmpty(department)
                || isEmpty(yearText)) {

            out.print(
                    "{\"success\":false,\"message\":\"Please fill all fields\"}"
            );
            return;
        }

        int id = Integer.parseInt(idText);
        int year = Integer.parseInt(yearText);

        if (id <= 0) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid student ID\"}"
            );
            return;
        }

        if (year < 1 || year > 10) {

            out.print(
                    "{\"success\":false,\"message\":\"Please enter a valid year\"}"
            );
            return;
        }

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String check =
                    "SELECT id FROM students " +
                    "WHERE (roll_number = ? OR LOWER(email) = LOWER(?)) " +
                    "AND id <> ?";

            try (PreparedStatement ps = con.prepareStatement(check)) {

                ps.setString(1, roll.trim());
                ps.setString(2, email.trim());
                ps.setInt(3, id);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        out.print(
                                "{\"success\":false,\"message\":\"Roll number or email already exists\"}"
                        );
                        return;
                    }
                }
            }

            String sql =
                    "UPDATE students SET " +
                    "student_name=?, " +
                    "roll_number=?, " +
                    "email=?, " +
                    "department=?, " +
                    "year=? " +
                    "WHERE id=?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name.trim());
                ps.setString(2, roll.trim());
                ps.setString(3, email.trim());
                ps.setString(4, department.trim());
                ps.setInt(5, year);
                ps.setInt(6, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    out.print(
                            "{\"success\":true,\"message\":\"Student updated successfully\"}"
                    );

                } else {

                    out.print(
                            "{\"success\":false,\"message\":\"Student not found\"}"
                    );
                }
            }
        }
    }

    private void deleteStudent(HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        String idText = request.getParameter("id");

        if (isEmpty(idText)) {

            out.print(
                    "{\"success\":false,\"message\":\"Student ID is required\"}"
            );
            return;
        }

        int id = Integer.parseInt(idText);

        if (id <= 0) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid student ID\"}"
            );
            return;
        }

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String sql = "DELETE FROM students WHERE id=?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    out.print(
                            "{\"success\":true,\"message\":\"Student deleted successfully\"}"
                    );

                } else {

                    out.print(
                            "{\"success\":false,\"message\":\"Student not found\"}"
                    );
                }
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {

            out.print(
                    "{\"success\":false,\"message\":\"This student has attendance or marks records and cannot be deleted. You can update the student instead.\"}"
            );
        }
    }

    private boolean isEmpty(String value) {

        return value == null || value.trim().isEmpty();
    }

    private String escapeJson(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}