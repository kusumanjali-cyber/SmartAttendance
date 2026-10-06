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

@WebServlet("/StudentAttendanceServlet")
public class StudentAttendanceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_HOST = System.getenv("DB_HOST");
    private static final String DB_PORT = System.getenv("DB_PORT");
    private static final String DB_NAME = System.getenv("DB_NAME");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final String DB_URL =
            "jdbc:mysql://" +
            DB_HOST + ":" +
            DB_PORT + "/" +
            DB_NAME +
            "?sslMode=REQUIRED";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login first");
            return;
        }

        Object usernameObj = session.getAttribute("username");

        if (usernameObj == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login first");
            return;
        }

        String username = String.valueOf(usernameObj).trim();

        if (username.isEmpty()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login first");
            return;
        }

        /*
         * Check database configuration.
         */
        if (DB_HOST == null || DB_HOST.trim().isEmpty()
                || DB_PORT == null || DB_PORT.trim().isEmpty()
                || DB_NAME == null || DB_NAME.trim().isEmpty()
                || DB_USER == null || DB_USER.trim().isEmpty()
                || DB_PASSWORD == null || DB_PASSWORD.trim().isEmpty()) {

            System.err.println(
                    "StudentAttendanceServlet: Database environment variables are missing."
            );

            sendError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database configuration is missing");

            return;
        }

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD)) {

                /*
                 * First find the student through users -> students.
                 *
                 * This does NOT depend on email.
                 */
                String studentSql =
                        "SELECT s.id, s.student_name " +
                        "FROM users u " +
                        "INNER JOIN students s ON s.user_id = u.id " +
                        "WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(?)) " +
                        "AND UPPER(TRIM(u.role)) = 'STUDENT' " +
                        "LIMIT 1";

                int studentId = -1;
                String studentName = null;

                try (PreparedStatement ps =
                             con.prepareStatement(studentSql)) {

                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {

                            studentId = rs.getInt("id");
                            studentName = rs.getString("student_name");
                        }
                    }
                }

                /*
                 * If username did not match, try the session email.
                 * This helps when older registrations stored email
                 * differently in users.username.
                 */
                if (studentId == -1) {

                    Object emailObj = session.getAttribute("email");

                    if (emailObj != null) {

                        String email = String.valueOf(emailObj).trim();

                        if (!email.isEmpty()) {

                            String emailSql =
                                    "SELECT s.id, s.student_name " +
                                    "FROM users u " +
                                    "INNER JOIN students s ON s.user_id = u.id " +
                                    "WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(?)) " +
                                    "AND UPPER(TRIM(u.role)) = 'STUDENT' " +
                                    "LIMIT 1";

                            try (PreparedStatement ps =
                                         con.prepareStatement(emailSql)) {

                                ps.setString(1, email);

                                try (ResultSet rs = ps.executeQuery()) {

                                    if (rs.next()) {

                                        studentId = rs.getInt("id");
                                        studentName =
                                                rs.getString("student_name");
                                    }
                                }
                            }
                        }
                    }
                }

                if (studentId == -1) {

                    sendError(response,
                            HttpServletResponse.SC_NOT_FOUND,
                            "Student record not found");

                    return;
                }

                /*
                 * Get attendance separately.
                 *
                 * This avoids multiplying attendance rows through
                 * joins with other tables.
                 */
                String attendanceSql =
                        "SELECT " +
                        "COALESCE(SUM(total_classes), 0) AS total_classes, " +
                        "COALESCE(SUM(attended_classes), 0) AS attended_classes " +
                        "FROM attendance_records " +
                        "WHERE student_id = ?";

                int totalClasses = 0;
                int attendedClasses = 0;

                try (PreparedStatement ps =
                             con.prepareStatement(attendanceSql)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {

                            totalClasses =
                                    rs.getInt("total_classes");

                            attendedClasses =
                                    rs.getInt("attended_classes");
                        }
                    }
                }

                /*
                 * Safety checks.
                 */
                if (totalClasses < 0) {
                    totalClasses = 0;
                }

                if (attendedClasses < 0) {
                    attendedClasses = 0;
                }

                if (attendedClasses > totalClasses) {
                    attendedClasses = totalClasses;
                }

                int absentClasses =
                        totalClasses - attendedClasses;

                double percentage = 0.0;

                if (totalClasses > 0) {

                    percentage =
                            ((double) attendedClasses
                                    / (double) totalClasses) * 100.0;
                }

                /*
                 * Number of future classes required to reach 75%.
                 *
                 * (attended + x) / (total + x) >= 0.75
                 *
                 * x >= 3*total - 4*attended
                 */
                int neededClasses = 0;

                if (percentage < 75.0) {

                    neededClasses =
                            (3 * totalClasses)
                            - (4 * attendedClasses);

                    if (neededClasses < 0) {
                        neededClasses = 0;
                    }
                }

                StringBuilder json =
                        new StringBuilder();

                json.append("{");

                json.append("\"success\":true,");

                json.append("\"studentName\":\"")
                        .append(escapeJson(studentName))
                        .append("\",");

                json.append("\"totalClasses\":")
                        .append(totalClasses)
                        .append(",");

                json.append("\"attendedClasses\":")
                        .append(attendedClasses)
                        .append(",");

                json.append("\"absentClasses\":")
                        .append(absentClasses)
                        .append(",");

                json.append("\"percentage\":")
                        .append(String.format(
                                java.util.Locale.US,
                                "%.2f",
                                percentage))
                        .append(",");

                json.append("\"neededClasses\":")
                        .append(neededClasses);

                json.append("}");

                out.print(json.toString());
            }

        } catch (Exception e) {

            System.err.println(
                    "StudentAttendanceServlet ERROR: "
                    + e.getMessage()
            );

            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error while loading attendance"
            );
        }
    }

    private void sendError(HttpServletResponse response,
                           int status,
                           String message)
            throws IOException {

        response.setStatus(status);

        response.getWriter().print(
                "{\"success\":false,\"message\":\""
                        + escapeJson(message)
                        + "\"}"
        );
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