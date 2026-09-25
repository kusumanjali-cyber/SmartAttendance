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

@WebServlet("/FacultyStudentsServlet")
public class FacultyStudentsServlet extends HttpServlet {

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

        String sql =
                "SELECT "
                + "id, "
                + "student_name, "
                + "roll_number, "
                + "email, "
                + "department, "
                + "section "
                + "FROM students "
                + "ORDER BY student_name";

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                Connection con =
                        DriverManager.getConnection(
                                DB_URL,
                                DB_USER,
                                DB_PASSWORD
                        );

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
            ) {

                StringBuilder json =
                        new StringBuilder();

                json.append(
                        "{\"success\":true,\"students\":["
                );

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    int id =
                            rs.getInt("id");

                    String name =
                            rs.getString("student_name");

                    String rollNumber =
                            rs.getString("roll_number");

                    String email =
                            rs.getString("email");

                    String department =
                            rs.getString("department");

                    String section =
                            rs.getString("section");

                    json.append("{");

                    json.append("\"id\":")
                            .append(id)
                            .append(",");

                    json.append("\"name\":\"")
                            .append(escapeJson(name))
                            .append("\",");

                    json.append("\"rollNumber\":\"")
                            .append(escapeJson(rollNumber))
                            .append("\",");

                    json.append("\"email\":\"")
                            .append(escapeJson(email))
                            .append("\",");

                    json.append("\"department\":\"")
                            .append(escapeJson(department))
                            .append("\",");

                    json.append("\"section\":\"")
                            .append(escapeJson(section))
                            .append("\"");

                    json.append("}");
                }

                json.append("]}");

                out.print(json.toString());
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Database error while loading students\"}"
            );
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
    }
}