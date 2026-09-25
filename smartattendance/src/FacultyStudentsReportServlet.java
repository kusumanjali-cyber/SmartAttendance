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

@WebServlet("/FacultyStudentsReportServlet")
public class FacultyStudentsReportServlet extends HttpServlet {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/smartattendance"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

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

        if (session == null || session.getAttribute("userId") == null) {
            out.print("{\"success\":false,\"message\":\"Please login first\"}");
            return;
        }

        String branch = request.getParameter("branch");
        String section = request.getParameter("section");
        String year = request.getParameter("year");

        if (branch == null || branch.trim().isEmpty()) {
            out.print("{\"success\":false,\"message\":\"Branch is required\"}");
            return;
        }

        if (section == null || section.trim().isEmpty()) {
            out.print("{\"success\":false,\"message\":\"Section is required\"}");
            return;
        }

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        sql.append("s.id, ");
        sql.append("s.student_name, ");
        sql.append("s.roll_number, ");
        sql.append("s.email, ");
        sql.append("s.department, ");
        sql.append("s.section, ");
        sql.append("s.year, ");

        sql.append("COALESCE(SUM(ar.total_classes), 0) AS total_classes, ");
        sql.append("COALESCE(SUM(ar.attended_classes), 0) AS attended_classes ");

        sql.append("FROM students s ");

        sql.append("LEFT JOIN attendance_records ar ");
        sql.append("ON ar.student_id = s.id ");

        sql.append("WHERE UPPER(TRIM(s.department)) = UPPER(TRIM(?)) ");
        sql.append("AND UPPER(TRIM(s.section)) = UPPER(TRIM(?)) ");

        boolean hasYear = year != null
                && !year.trim().isEmpty()
                && !year.equalsIgnoreCase("All");

        if (hasYear) {
            sql.append("AND CAST(s.year AS CHAR) = ? ");
        }

        sql.append("GROUP BY ");
        sql.append("s.id, ");
        sql.append("s.student_name, ");
        sql.append("s.roll_number, ");
        sql.append("s.email, ");
        sql.append("s.department, ");
        sql.append("s.section, ");
        sql.append("s.year ");

        sql.append("ORDER BY s.student_name ASC");

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );

            PreparedStatement ps = con.prepareStatement(sql.toString());

            ps.setString(1, branch.trim());
            ps.setString(2, section.trim());

            if (hasYear) {
                ps.setString(3, year.trim());
            }

            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder();

            json.append("{");
            json.append("\"success\":true,");
            json.append("\"students\":[");

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                int studentId = rs.getInt("id");

                String studentName = rs.getString("student_name");
                String rollNumber = rs.getString("roll_number");
                String email = rs.getString("email");
                String department = rs.getString("department");
                String studentSection = rs.getString("section");
                String studentYear = rs.getString("year");

                int totalClasses = rs.getInt("total_classes");
                int attendedClasses = rs.getInt("attended_classes");

                double percentage = 0.0;

                if (totalClasses > 0) {
                    percentage =
                            ((double) attendedClasses / totalClasses) * 100.0;
                }

                String status;

                if (percentage >= 75.0) {
                    status = "Good";
                } else {
                    status = "Low";
                }

                json.append("{");

                json.append("\"id\":")
                    .append(studentId)
                    .append(",");

                json.append("\"studentName\":\"")
                    .append(escape(studentName))
                    .append("\",");

                json.append("\"rollNumber\":\"")
                    .append(escape(rollNumber))
                    .append("\",");

                json.append("\"email\":\"")
                    .append(escape(email))
                    .append("\",");

                json.append("\"department\":\"")
                    .append(escape(department))
                    .append("\",");

                json.append("\"section\":\"")
                    .append(escape(studentSection))
                    .append("\",");

                json.append("\"year\":\"")
                    .append(escape(studentYear))
                    .append("\",");

                json.append("\"totalClasses\":")
                    .append(totalClasses)
                    .append(",");

                json.append("\"attendedClasses\":")
                    .append(attendedClasses)
                    .append(",");

                json.append("\"attendancePercentage\":")
                    .append(String.format(
                            java.util.Locale.US,
                            "%.2f",
                            percentage
                    ))
                    .append(",");

                json.append("\"status\":\"")
                    .append(status)
                    .append("\"");

                json.append("}");
            }

            json.append("]");
            json.append("}");

            out.print(json.toString());

            rs.close();
            ps.close();
            con.close();

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                "{\"success\":false,\"message\":\""
                + escape(e.getMessage())
                + "\"}"
            );
        }
    }

    private String escape(String value) {

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