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

        /*
         * Student must be logged in.
         */
        if (session == null
                || session.getAttribute("username") == null
                || session.getAttribute("passwordVerified") == null) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Please login first\"}"
            );

            return;
        }

        String username =
                String.valueOf(
                        session.getAttribute("username")
                );

        String sql =
                "SELECT "
                + "s.id, "
                + "s.student_name, "
                + "COALESCE(SUM(ar.total_classes), 0) AS total_classes, "
                + "COALESCE(SUM(ar.attended_classes), 0) AS attended_classes "
                + "FROM users u "
                + "INNER JOIN students s "
                + "ON LOWER(TRIM(s.email)) = LOWER(TRIM(u.username)) "
                + "LEFT JOIN attendance_records ar "
                + "ON ar.student_id = s.id "
                + "WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(?)) "
                + "AND u.role = 'STUDENT' "
                + "GROUP BY s.id, s.student_name";

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                Connection con = DriverManager.getConnection(
                        DB_URL,
                        DB_USER,
                        DB_PASSWORD
                );

                PreparedStatement ps =
                        con.prepareStatement(sql)
            ) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_NOT_FOUND
                        );

                        out.print(
                                "{\"success\":false,"
                                + "\"message\":\"Student record not found\"}"
                        );

                        return;
                    }

                    int totalClasses =
                            rs.getInt("total_classes");

                    int attendedClasses =
                            rs.getInt("attended_classes");

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
                                / totalClasses) * 100.0;
                    }

                    /*
                     * Calculate the number of FUTURE classes
                     * the student needs to attend continuously
                     * to reach 75%.
                     *
                     * Formula:
                     *
                     * (attended + x) / (total + x) >= 0.75
                     *
                     * x >= 3 * total - 4 * attended
                     */
                    int neededClasses = 0;

                    if (percentage < 75.0 && totalClasses > 0) {

                        neededClasses =
                                (3 * totalClasses)
                                - (4 * attendedClasses);

                        if (neededClasses < 0) {
                            neededClasses = 0;
                        }
                    }

                    String studentName =
                            rs.getString("student_name");

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
                            .append(
                                    String.format(
                                            java.util.Locale.US,
                                            "%.2f",
                                            percentage
                                    )
                            )
                            .append(",");

                    json.append("\"neededClasses\":")
                            .append(neededClasses);

                    json.append("}");

                    out.print(json.toString());
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Database error while loading attendance\"}"
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