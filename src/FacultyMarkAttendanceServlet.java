import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/FacultyMarkAttendanceServlet")
public class FacultyMarkAttendanceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/smartattendance"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();

        /* =====================================================
           SESSION CHECK
           ===================================================== */

        HttpSession session =
                request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null ||
            !Boolean.TRUE.equals(
                session.getAttribute("passwordVerified"))) {

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Session expired. Please login again.\"}"
            );

            return;
        }

        String studentIdStr =
                request.getParameter("studentId");

        String subjectIdStr =
                request.getParameter("subjectId");

        String status =
                request.getParameter("status");

        try {

            /* =================================================
               VALIDATION
               ================================================= */

            if (studentIdStr == null ||
                subjectIdStr == null ||
                status == null ||
                studentIdStr.trim().isEmpty() ||
                subjectIdStr.trim().isEmpty()) {

                out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Student, subject and attendance status are required.\"}"
                );

                return;
            }

            int studentId =
                    Integer.parseInt(
                        studentIdStr.trim()
                    );

            int subjectId =
                    Integer.parseInt(
                        subjectIdStr.trim()
                    );

            status =
                    status.trim().toUpperCase();

            if (!status.equals("PRESENT") &&
                !status.equals("ABSENT")) {

                out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Invalid attendance status.\"}"
                );

                return;
            }

            /*
             * IMPORTANT:
             * Faculty does NOT send the date.
             * Server automatically uses today's date.
             */

            LocalDate today =
                    LocalDate.now();

            java.sql.Date attendanceDate =
                    java.sql.Date.valueOf(today);

            boolean present =
                    "PRESENT".equals(status);

            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );

            try (Connection con =
                    DriverManager.getConnection(
                            DB_URL,
                            DB_USER,
                            DB_PASSWORD)) {

                /* =============================================
                   CHECK WHETHER TODAY'S RECORD EXISTS
                   ============================================= */

                String checkSql =
                        "SELECT id "
                        + "FROM attendance_records "
                        + "WHERE student_id = ? "
                        + "AND subject_id = ? "
                        + "AND attendance_date = ?";

                int existingId = -1;

                try (PreparedStatement ps =
                        con.prepareStatement(checkSql)) {

                    ps.setInt(1, studentId);

                    ps.setInt(2, subjectId);

                    ps.setDate(
                            3,
                            attendanceDate
                    );

                    try (ResultSet rs =
                            ps.executeQuery()) {

                        if (rs.next()) {

                            existingId =
                                    rs.getInt("id");
                        }
                    }
                }

                /* =============================================
                   UPDATE TODAY'S RECORD
                   ============================================= */

                if (existingId != -1) {

                    String updateSql =
                            "UPDATE attendance_records "
                            + "SET total_classes = 1, "
                            + "attended_classes = ? "
                            + "WHERE id = ?";

                    try (PreparedStatement ps =
                            con.prepareStatement(updateSql)) {

                        ps.setInt(
                                1,
                                present ? 1 : 0
                        );

                        ps.setInt(
                                2,
                                existingId
                        );

                        ps.executeUpdate();
                    }

                    out.print(
                        "{\"success\":true,"
                        + "\"message\":\"Today's attendance updated successfully.\","
                        + "\"attendanceDate\":\""
                        + today
                        + "\"}"
                    );

                } else {

                    /* =========================================
                       INSERT TODAY'S RECORD
                       ========================================= */

                    String insertSql =
                            "INSERT INTO attendance_records "
                            + "(student_id, subject_id, "
                            + "total_classes, attended_classes, "
                            + "attendance_date) "
                            + "VALUES (?, ?, 1, ?, ?)";

                    try (PreparedStatement ps =
                            con.prepareStatement(insertSql)) {

                        ps.setInt(
                                1,
                                studentId
                        );

                        ps.setInt(
                                2,
                                subjectId
                        );

                        ps.setInt(
                                3,
                                present ? 1 : 0
                        );

                        ps.setDate(
                                4,
                                attendanceDate
                        );

                        ps.executeUpdate();
                    }

                    out.print(
                        "{\"success\":true,"
                        + "\"message\":\"Today's attendance saved successfully.\","
                        + "\"attendanceDate\":\""
                        + today
                        + "\"}"
                    );
                }
            }

        } catch (NumberFormatException e) {

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Invalid student or subject ID.\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Database error: "
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
                .replace("\n", "\\n");
    }
}