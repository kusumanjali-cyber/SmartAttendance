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

@WebServlet("/AdminFacultyServlet")
public class AdminFacultyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/smartattendance"
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";

    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT id, faculty_name, faculty_id, email, department " +
                 "FROM faculty ORDER BY faculty_name");
             ResultSet rs = ps.executeQuery()) {

            PrintWriter out = response.getWriter();

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"faculty\":[");

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{");
                json.append("\"id\":").append(rs.getInt("id")).append(",");
                json.append("\"facultyName\":\"")
                    .append(escape(rs.getString("faculty_name"))).append("\",");
                json.append("\"facultyId\":\"")
                    .append(escape(rs.getString("faculty_id"))).append("\",");
                json.append("\"email\":\"")
                    .append(escape(rs.getString("email"))).append("\",");
                json.append("\"department\":\"")
                    .append(escape(rs.getString("department"))).append("\"");
                json.append("}");
            }

            json.append("]}");

            out.print(json.toString());

        } catch (Exception e) {

            response.setStatus(500);

            response.getWriter().print(
                "{\"success\":false,\"message\":\""
                + escape(e.getMessage()) + "\"}"
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try (Connection con = getConnection()) {

            if ("add".equalsIgnoreCase(action)) {

                String name = request.getParameter("facultyName");
                String facultyId = request.getParameter("facultyId");
                String email = request.getParameter("email");
                String department = request.getParameter("department");

                if (isEmpty(name) || isEmpty(facultyId)
                        || isEmpty(email) || isEmpty(department)) {

                    send(response, false, "Please fill all fields.");
                    return;
                }

                String sql =
                    "INSERT INTO faculty " +
                    "(faculty_name, faculty_id, email, department) " +
                    "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setString(1, name.trim());
                    ps.setString(2, facultyId.trim());
                    ps.setString(3, email.trim());
                    ps.setString(4, department.trim());

                    ps.executeUpdate();
                }

                send(response, true, "Faculty added successfully.");
                return;
            }

            if ("update".equalsIgnoreCase(action)) {

                String id = request.getParameter("id");
                String name = request.getParameter("facultyName");
                String facultyId = request.getParameter("facultyId");
                String email = request.getParameter("email");
                String department = request.getParameter("department");

                if (isEmpty(id) || isEmpty(name) || isEmpty(facultyId)
                        || isEmpty(email) || isEmpty(department)) {

                    send(response, false, "Please fill all fields.");
                    return;
                }

                String sql =
                    "UPDATE faculty SET faculty_name=?, faculty_id=?, " +
                    "email=?, department=? WHERE id=?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setString(1, name.trim());
                    ps.setString(2, facultyId.trim());
                    ps.setString(3, email.trim());
                    ps.setString(4, department.trim());
                    ps.setInt(5, Integer.parseInt(id));

                    ps.executeUpdate();
                }

                send(response, true, "Faculty updated successfully.");
                return;
            }

            if ("delete".equalsIgnoreCase(action)) {

                String id = request.getParameter("id");

                if (isEmpty(id)) {
                    send(response, false, "Faculty ID is required.");
                    return;
                }

                String sql = "DELETE FROM faculty WHERE id=?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, Integer.parseInt(id));
                    ps.executeUpdate();
                }

                send(response, true, "Faculty deleted successfully.");
                return;
            }

            send(response, false, "Invalid action.");

        } catch (Exception e) {

            response.setStatus(500);

            send(response, false, e.getMessage());
        }
    }

    private void send(HttpServletResponse response,
                      boolean success,
                      String message) throws IOException {

        response.getWriter().print(
            "{\"success\":" + success +
            ",\"message\":\"" + escape(message) + "\"}"
        );
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}