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

@WebServlet("/AttendanceServlet")
public class AttendanceServlet extends HttpServlet {

    private static final String DB_URL =
        "jdbc:mysql://smartattendance-db-kusumanjaligadupudi-ef99.d.aivencloud.com:21100/defaultdb"
        + "?sslMode=REQUIRED"
        + "&serverTimezone=UTC";

    private static final String DB_USER = "avnadmin";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String type = request.getParameter("type");

        if (type == null) {
            sendError(response, "Missing type");
            return;
        }

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            if ("subjects".equalsIgnoreCase(type)) {

                getSubjects(response);

            } else if ("students".equalsIgnoreCase(type)) {

                getStudents(request, response);

            } else if ("records".equalsIgnoreCase(type)) {

                getRecords(request, response);

            } else {

                sendError(response, "Invalid type");
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            sendError(
                    response,
                    "Database error: " + e.getMessage()
            );
        }
    }


    /* =========================
       SUBJECTS
       ========================= */

    private void getSubjects(
            HttpServletResponse response)
            throws Exception {

        String sql =
                "SELECT id, subject_name "
                + "FROM subjects "
                + "ORDER BY subject_name";

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

            json.append("[");

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    json.append(",");
                }

                json.append("{");

                json.append("\"id\":")
                     .append(rs.getInt("id"))
                     .append(",");

                json.append("\"subjectName\":\"")
                     .append(
                         escapeJson(
                             rs.getString("subject_name")
                         )
                     )
                     .append("\"");

                json.append("}");

                first = false;
            }

            json.append("]");

            response.getWriter().write(
                    json.toString()
            );
        }
    }


    /* =========================
       STUDENTS
       ========================= */

    private void getStudents(
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        String branch =
                request.getParameter("branch");

        String section =
                request.getParameter("section");

        String sql =
                "SELECT id, student_name, roll_number, "
                + "email, department, section, year "
                + "FROM students ";

        boolean hasBranch =
                branch != null &&
                !branch.trim().isEmpty();

        boolean hasSection =
                section != null &&
                !section.trim().isEmpty();

        if (hasBranch && hasSection) {

            sql +=
                "WHERE department = ? "
                + "AND section = ? ";

        } else if (hasBranch) {

            sql +=
                "WHERE department = ? ";

        } else if (hasSection) {

            sql +=
                "WHERE section = ";
        }

        sql +=
                "ORDER BY student_name";

        try (
            Connection con =
                    DriverManager.getConnection(
                            DB_URL,
                            DB_USER,
                            DB_PASSWORD
                    );

            PreparedStatement ps =
                    con.prepareStatement(sql)
        ) {

            int index = 1;

            if (hasBranch) {

                ps.setString(
                        index++,
                        branch
                );
            }

            if (hasSection) {

                ps.setString(
                        index++,
                        section
                );
            }

            try (
                ResultSet rs =
                        ps.executeQuery()
            ) {

                StringBuilder json =
                        new StringBuilder();

                json.append("[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{");

                    json.append("\"id\":")
                         .append(rs.getInt("id"))
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

                    json.append("\"email\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("email")
                             )
                         )
                         .append("\",");

                    json.append("\"department\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("department")
                             )
                         )
                         .append("\",");

                    json.append("\"section\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("section")
                             )
                         )
                         .append("\",");

                    json.append("\"year\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("year")
                             )
                         )
                         .append("\"");

                    json.append("}");

                    first = false;
                }

                json.append("]");

                response.getWriter().write(
                        json.toString()
                );
            }
        }
    }


    /* =========================
       ATTENDANCE RECORDS
       ========================= */

    private void getRecords(
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        javax.servlet.http.HttpSession session =
                request.getSession(false);

        String facultySubject =
                session == null ? "" :
                (String) session.getAttribute("subject");

        String sql =
                "SELECT ar.id, "
                + "ar.student_id, "
                + "ar.subject_id, "
                + "ar.total_classes, "
                + "ar.attended_classes, "
                + "ar.attendance_date, "
                + "s.student_name, "
                + "s.roll_number, "
                + "s.department, "
                + "s.section, "
                + "sub.subject_name "
                + "FROM attendance_records ar "
                + "JOIN students s "
                + "ON ar.student_id = s.id "
                + "JOIN subjects sub "
                + "ON ar.subject_id = sub.id "
                + "WHERE LOWER(TRIM(sub.subject_name)) = LOWER(TRIM(?)) "
                + "ORDER BY ar.attendance_date DESC";

        try (
            Connection con =
                    DriverManager.getConnection(
                            DB_URL,
                            DB_USER,
                            DB_PASSWORD
                    );

            PreparedStatement ps =
                    con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    facultySubject
            );

            try (
                ResultSet rs =
                        ps.executeQuery()
            ) {

                StringBuilder json =
                        new StringBuilder();

                json.append("[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{");

                    json.append("\"id\":")
                         .append(rs.getInt("id"))
                         .append(",");

                    json.append("\"studentId\":")
                         .append(rs.getInt("student_id"))
                         .append(",");

                    json.append("\"subjectId\":")
                         .append(rs.getInt("subject_id"))
                         .append(",");

                    json.append("\"totalClasses\":")
                         .append(rs.getInt("total_classes"))
                         .append(",");

                    json.append("\"attendedClasses\":")
                         .append(rs.getInt("attended_classes"))
                         .append(",");

                    json.append("\"attendanceDate\":\"")
                         .append(
                             escapeJson(
                                 String.valueOf(
                                     rs.getDate("attendance_date")
                                 )
                             )
                         )
                         .append("\",");

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

                    json.append("\"department\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("department")
                             )
                         )
                         .append("\",");

                    json.append("\"section\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("section")
                             )
                         )
                         .append("\",");

                    json.append("\"subjectName\":\"")
                         .append(
                             escapeJson(
                                 rs.getString("subject_name")
                             )
                         )
                         .append("\"");

                    json.append("}");

                    first = false;
                }

                json.append("]");

                response.getWriter().write(
                        json.toString()
                );
            }
        }
    }


    /* =========================
       JSON ESCAPE
       ========================= */

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }


    /* =========================
       ERROR
       ========================= */

    private void sendError(
            HttpServletResponse response,
            String message)
            throws IOException {

        response.getWriter().write(
                "{\"error\":\""
                + escapeJson(message)
                + "\"}"
        );
    }
}