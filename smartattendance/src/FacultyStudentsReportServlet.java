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

@WebServlet("/FacultyStudentsReportServlet")
public class FacultyStudentsReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String branch = request.getParameter("branch");
        String section = request.getParameter("section");
        String year = request.getParameter("year");

        boolean hasBranch =
                branch != null && !branch.trim().isEmpty();

        boolean hasSection =
                section != null && !section.trim().isEmpty();

        boolean hasYear =
                year != null
                && !year.trim().isEmpty()
                && !year.equalsIgnoreCase("All");

        /*
         * Attendance:
         * attendance_records contains total_classes and attended_classes.
         *
         * Marks:
         * marks table contains individual examination marks.
         *
         * Marks percentage:
         * Mid 1 = 15
         * Mid 2 = 15
         * Course Activity = 70
         *
         * Only examinations which have marks entered are included.
         */

        StringBuilder sql = new StringBuilder();

        sql.append(
            "SELECT " +
            "s.id, " +
            "s.student_name, " +
            "s.roll_number, " +
            "s.email, " +
            "s.department, " +
            "s.section, " +
            "s.year, " +

            "COALESCE(SUM(ar.total_classes),0) AS total_classes, " +
            "COALESCE(SUM(ar.attended_classes),0) AS attended_classes, " +

            "COALESCE(SUM(m.marks),0) AS marks_obtained, " +

            "COALESCE(SUM( " +
                "CASE " +
                "WHEN LOWER(TRIM(m.exam_type)) IN " +
                "('mid 1','mid1','mid-1') THEN m.marks " +
                "WHEN LOWER(TRIM(m.exam_type)) IN " +
                "('mid 2','mid2','mid-2') THEN m.marks " +
                "WHEN LOWER(TRIM(m.exam_type)) IN " +
                "('course activity','courseactivity','course_activity') THEN m.marks " +
                "ELSE 0 " +
                "END" +
            "),0) AS entered_marks, " +

            "COUNT(m.id) AS entered_exams " +

            "FROM students s " +

            "LEFT JOIN attendance_records ar " +
            "ON ar.student_id = s.id " +

            "LEFT JOIN marks m " +
            "ON m.student_id = s.id "
        );

        boolean hasWhere = false;

        if (hasBranch) {

            sql.append(
                "WHERE UPPER(TRIM(s.department)) = " +
                "UPPER(TRIM(?)) "
            );

            hasWhere = true;
        }

        if (hasSection) {

            sql.append(
                hasWhere ? "AND " : "WHERE "
            );

            sql.append(
                "UPPER(TRIM(s.section)) = " +
                "UPPER(TRIM(?)) "
            );

            hasWhere = true;
        }

        if (hasYear) {

            sql.append(
                hasWhere ? "AND " : "WHERE "
            );

            sql.append(
                "CAST(s.year AS CHAR) = ? "
            );

            hasWhere = true;
        }

        sql.append(
            "GROUP BY " +
            "s.id, " +
            "s.student_name, " +
            "s.roll_number, " +
            "s.email, " +
            "s.department, " +
            "s.section, " +
            "s.year "
        );

        sql.append("ORDER BY s.student_name ASC");

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            if (DB_URL == null ||
                DB_USER == null ||
                DB_PASSWORD == null) {

                out.print(
                    "{\"success\":false," +
                    "\"message\":\"Database environment variables are missing\"}"
                );

                return;
            }

            try (
                Connection con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
                );

                PreparedStatement ps =
                    con.prepareStatement(sql.toString())
            ) {

                int parameterIndex = 1;

                if (hasBranch) {

                    ps.setString(
                        parameterIndex++,
                        branch.trim()
                    );
                }

                if (hasSection) {

                    ps.setString(
                        parameterIndex++,
                        section.trim()
                    );
                }

                if (hasYear) {

                    ps.setString(
                        parameterIndex++,
                        year.trim()
                    );
                }

                try (ResultSet rs = ps.executeQuery()) {

                    StringBuilder json =
                        new StringBuilder();

                    json.append("{");
                    json.append("\"success\":true,");
                    json.append("\"students\":[");

                    boolean first = true;

                    while (rs.next()) {

                        if (!first) {
                            json.append(",");
                        }

                        first = false;

                        int studentId =
                            rs.getInt("id");

                        String studentName =
                            rs.getString("student_name");

                        String rollNumber =
                            rs.getString("roll_number");

                        String email =
                            rs.getString("email");

                        String department =
                            rs.getString("department");

                        String studentSection =
                            rs.getString("section");

                        String studentYear =
                            rs.getString("year");

                        int totalClasses =
                            rs.getInt("total_classes");

                        int attendedClasses =
                            rs.getInt("attended_classes");

                        int absentClasses =
                            Math.max(
                                0,
                                totalClasses -
                                attendedClasses
                            );

                        double attendancePercentage = 0.0;

                        if (totalClasses > 0) {

                            attendancePercentage =
                                ((double) attendedClasses
                                / totalClasses) * 100.0;
                        }

                        /*
                         * Marks calculation
                         *
                         * Mid 1      = /15
                         * Mid 2      = /15
                         * Course Act = /70
                         *
                         * Convert entered marks to percentage
                         * based only on examinations entered.
                         */

                        double mid1Marks = 0.0;
                        double mid2Marks = 0.0;
                        double courseActivityMarks = 0.0;

                        boolean mid1Entered = false;
                        boolean mid2Entered = false;
                        boolean courseActivityEntered = false;

                        /*
                         * We need the individual examination
                         * values, so run a small query for this
                         * student.
                         */

                        String marksSql =
                            "SELECT exam_type, marks " +
                            "FROM marks " +
                            "WHERE student_id = ?";

                        try (
                            PreparedStatement marksPs =
                                con.prepareStatement(marksSql)
                        ) {

                            marksPs.setInt(
                                1,
                                studentId
                            );

                            try (
                                ResultSet marksRs =
                                    marksPs.executeQuery()
                            ) {

                                while (marksRs.next()) {

                                    String examType =
                                        marksRs.getString("exam_type");

                                    double mark =
                                        marksRs.getDouble("marks");

                                    if (examType == null) {
                                        continue;
                                    }

                                    String normalized =
                                        examType
                                        .trim()
                                        .toLowerCase()
                                        .replace("-", " ")
                                        .replace("_", " ");

                                    if (
                                        normalized.equals("mid 1") ||
                                        normalized.equals("mid1")
                                    ) {

                                        mid1Marks = mark;
                                        mid1Entered = true;

                                    } else if (
                                        normalized.equals("mid 2") ||
                                        normalized.equals("mid2")
                                    ) {

                                        mid2Marks = mark;
                                        mid2Entered = true;

                                    } else if (
                                        normalized.equals("course activity") ||
                                        normalized.equals("courseactivity")
                                    ) {

                                        courseActivityMarks = mark;
                                        courseActivityEntered = true;
                                    }
                                }
                            }
                        }

                        double totalEnteredMarks = 0.0;
                        double totalMaximumMarks = 0.0;

                        if (mid1Entered) {

                            totalEnteredMarks += mid1Marks;
                            totalMaximumMarks += 15.0;
                        }

                        if (mid2Entered) {

                            totalEnteredMarks += mid2Marks;
                            totalMaximumMarks += 15.0;
                        }

                        if (courseActivityEntered) {

                            totalEnteredMarks += courseActivityMarks;
                            totalMaximumMarks += 70.0;
                        }

                        double marksPercentage = 0.0;

                        if (totalMaximumMarks > 0) {

                            marksPercentage =
                                (totalEnteredMarks /
                                totalMaximumMarks) * 100.0;
                        }

                        boolean marksEntered =
                            totalMaximumMarks > 0;

                        String status;

                        if (!marksEntered &&
                            totalClasses == 0) {

                            status = "Not entered";

                        } else if (
                            attendancePercentage < 75.0 ||
                            (marksEntered &&
                             marksPercentage < 40.0)
                        ) {

                            status = "At Risk";

                        } else {

                            status = "Safe";
                        }

                        json.append("{");

                        json.append("\"id\":")
                            .append(studentId)
                            .append(",");

                        json.append("\"studentName\":\"")
                            .append(escape(studentName))
                            .append("\",");

                        json.append("\"name\":\"")
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

                        json.append("\"total_classes\":")
                            .append(totalClasses)
                            .append(",");

                        json.append("\"attendedClasses\":")
                            .append(attendedClasses)
                            .append(",");

                        json.append("\"attended_classes\":")
                            .append(attendedClasses)
                            .append(",");

                        json.append("\"absentClasses\":")
                            .append(absentClasses)
                            .append(",");

                        json.append("\"absent_classes\":")
                            .append(absentClasses)
                            .append(",");

                        json.append("\"attendancePercentage\":")
                            .append(format(attendancePercentage))
                            .append(",");

                        json.append("\"attendance_percentage\":")
                            .append(format(attendancePercentage))
                            .append(",");

                        json.append("\"marks\":")
                            .append(format(totalEnteredMarks))
                            .append(",");

                        json.append("\"marksObtained\":")
                            .append(format(totalEnteredMarks))
                            .append(",");

                        json.append("\"marksPercentage\":")
                            .append(format(marksPercentage))
                            .append(",");

                        json.append("\"marks_percentage\":")
                            .append(format(marksPercentage))
                            .append(",");

                        json.append("\"enteredMarks\":")
                            .append(format(totalEnteredMarks))
                            .append(",");

                        json.append("\"maximumMarks\":")
                            .append(format(totalMaximumMarks))
                            .append(",");

                        json.append("\"marksEntered\":")
                            .append(marksEntered)
                            .append(",");

                        json.append("\"status\":\"")
                            .append(escape(status))
                            .append("\"");

                        json.append("}");
                    }

                    json.append("]");
                    json.append("}");

                    out.print(json.toString());
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                "{\"success\":false," +
                "\"message\":\"" +
                escape(
                    e.getMessage() == null
                    ? "Database error"
                    : e.getMessage()
                ) +
                "\"}"
            );
        }
    }

    private String format(double value) {

        return String.format(
            java.util.Locale.US,
            "%.2f",
            value
        );
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