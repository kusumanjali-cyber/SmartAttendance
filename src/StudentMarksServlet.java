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
import javax.servlet.http.HttpSession;

@WebServlet("/StudentMarksServlet")
public class StudentMarksServlet extends HttpServlet {

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

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null ||
            session.getAttribute("passwordVerified") == null) {

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Please login first.\"}"
            );
            return;
        }

        Object userIdObject = session.getAttribute("userId");

        int userId;

        try {
            userId = Integer.parseInt(userIdObject.toString());
        } catch (Exception e) {

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Invalid user session.\"}"
            );
            return;
        }

        String sql =
                "SELECT "
                + "m.id, "
                + "m.student_id, "
                + "s.student_name, "
                + "s.roll_number, "
                + "m.subject_id, "
                + "sub.subject_name, "
                + "m.exam_type, "
                + "m.marks "
                + "FROM marks m "
                + "JOIN students s "
                + "ON m.student_id = s.id "
                + "JOIN subjects sub "
                + "ON m.subject_id = sub.id "
                + "JOIN users u "
                + "ON u.username = s.email "
                + "WHERE u.id = ? "
                + "ORDER BY sub.subject_name, m.exam_type";

        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"success\":true,");
        json.append("\"marks\":[");

        boolean first = true;

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con =
                        DriverManager.getConnection(
                            DB_URL,
                            DB_USER,
                            DB_PASSWORD);

                 PreparedStatement ps =
                        con.prepareStatement(sql)) {

                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        if (!first) {
                            json.append(",");
                        }

                        first = false;

                        json.append("{");

                        json.append("\"id\":")
                            .append(rs.getInt("id"))
                            .append(",");

                        json.append("\"studentId\":")
                            .append(rs.getInt("student_id"))
                            .append(",");

                        json.append("\"studentName\":\"")
                            .append(
                                escapeJson(
                                    rs.getString("student_name")
                                )
                            )
                            .append("\",");

                        json.append("\"rollNumber\":\"")
                            .append(
                                escapeJson(
                                    rs.getString("roll_number")
                                )
                            )
                            .append("\",");

                        json.append("\"subjectId\":")
                            .append(rs.getInt("subject_id"))
                            .append(",");

                        json.append("\"subjectName\":\"")
                            .append(
                                escapeJson(
                                    rs.getString("subject_name")
                                )
                            )
                            .append("\",");

                        json.append("\"examType\":\"")
                            .append(
                                escapeJson(
                                    rs.getString("exam_type")
                                )
                            )
                            .append("\",");

                        json.append("\"marks\":")
                            .append(rs.getDouble("marks"));

                        json.append("}");
                    }
                }
            }

            json.append("]");
            json.append("}");

            out.print(json.toString());

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                "{\"success\":false,"
                + "\"message\":\""
                + escapeJson(e.getMessage())
                + "\"}"
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
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}