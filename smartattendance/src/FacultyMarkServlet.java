import java.io.BufferedReader;
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

@WebServlet("/FacultyMarkServlet")
public class FacultyMarkServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/smartattendance"
        + "?useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";


    // =========================================================
    // GET - LOAD MARKS
    // =========================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String sql =
            "SELECT m.id, "
            + "m.student_id, "
            + "s.student_name, "
            + "s.roll_number, "
            + "m.subject_id, "
            + "sub.subject_name, "
            + "m.exam_type, "
            + "m.marks "
            + "FROM marks m "
            + "JOIN students s ON m.student_id = s.id "
            + "JOIN subjects sub ON m.subject_id = sub.id "
            + "ORDER BY m.id DESC";

        StringBuilder json = new StringBuilder();

        json.append("{\"success\":true,\"marks\":[");

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                Connection con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
                );

                PreparedStatement ps =
                    con.prepareStatement(sql);

                ResultSet rs = ps.executeQuery()
            ) {

                boolean first = true;

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

            json.append("]}");

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


    // =========================================================
    // POST - SAVE / UPDATE MARKS + SEND EMAIL
    // =========================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {

            // -------------------------------------------------
            // READ REQUEST
            // -------------------------------------------------

            String contentType =
                request.getContentType();

            String studentIdText;
            String subjectIdText;
            String marksText;

            String examType = "Internal";


            if (contentType != null
                    && contentType.toLowerCase()
                    .contains("application/json")) {

                StringBuilder body =
                    new StringBuilder();

                BufferedReader reader =
                    request.getReader();

                String line;

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                String json = body.toString();

                studentIdText =
                    getJsonValue(json, "studentId");

                subjectIdText =
                    getJsonValue(json, "subjectId");

                marksText =
                    getJsonValue(json, "marks");

                String jsonExamType =
                    getJsonValue(json, "examType");

                if (jsonExamType != null
                        && !jsonExamType.trim().isEmpty()) {

                    examType =
                        jsonExamType.trim();
                }

            } else {

                studentIdText =
                    request.getParameter("studentId");

                subjectIdText =
                    request.getParameter("subjectId");

                marksText =
                    request.getParameter("marks");

                String formExamType =
                    request.getParameter("examType");

                if (formExamType != null
                        && !formExamType.trim().isEmpty()) {

                    examType =
                        formExamType.trim();
                }
            }


            // -------------------------------------------------
            // VALIDATION
            // -------------------------------------------------

            if (studentIdText == null
                    || subjectIdText == null
                    || marksText == null
                    || studentIdText.trim().isEmpty()
                    || subjectIdText.trim().isEmpty()
                    || marksText.trim().isEmpty()) {

                out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Student, subject and marks are required.\"}"
                );

                return;
            }


            int studentId =
                Integer.parseInt(
                    studentIdText.trim()
                );

            int subjectId =
                Integer.parseInt(
                    subjectIdText.trim()
                );

            double marks =
                Double.parseDouble(
                    marksText.trim()
                );


            if (marks < 0 || marks > 100) {

                out.print(
                    "{\"success\":false,"
                    + "\"message\":\"Marks must be between 0 and 100.\"}"
                );

                return;
            }


            // -------------------------------------------------
            // LOAD STUDENT EMAIL + NAME + SUBJECT
            // -------------------------------------------------

            Class.forName(
                "com.mysql.cj.jdbc.Driver"
            );

            try (
                Connection con =
                    DriverManager.getConnection(
                        DB_URL,
                        DB_USER,
                        DB_PASSWORD
                    )
            ) {

                String studentSql =
                    "SELECT "
                    + "s.student_name, "
                    + "s.email, "
                    + "sub.subject_name "
                    + "FROM students s "
                    + "JOIN subjects sub "
                    + "ON sub.id = ? "
                    + "WHERE s.id = ?";

                String studentName = "";
                String studentEmail = "";
                String subjectName = "";

                try (
                    PreparedStatement ps =
                        con.prepareStatement(studentSql)
                ) {

                    ps.setInt(1, subjectId);
                    ps.setInt(2, studentId);

                    try (
                        ResultSet rs =
                            ps.executeQuery()
                    ) {

                        if (rs.next()) {

                            studentName =
                                rs.getString(
                                    "student_name"
                                );

                            studentEmail =
                                rs.getString(
                                    "email"
                                );

                            subjectName =
                                rs.getString(
                                    "subject_name"
                                );
                        }
                    }
                }


                // -------------------------------------------------
                // CHECK STUDENT
                // -------------------------------------------------

                if (studentName == null
                        || studentName.trim().isEmpty()) {

                    out.print(
                        "{\"success\":false,"
                        + "\"message\":\"Student not found.\"}"
                    );

                    return;
                }

                if (studentEmail == null) {
                    studentEmail = "";
                }

                if (subjectName == null) {
                    subjectName = "";
                }


                // -------------------------------------------------
                // CHECK EXISTING MARK
                // -------------------------------------------------

                String checkSql =
                    "SELECT id FROM marks "
                    + "WHERE student_id = ? "
                    + "AND subject_id = ? "
                    + "AND exam_type = ?";

                int existingId = -1;

                try (
                    PreparedStatement ps =
                        con.prepareStatement(checkSql)
                ) {

                    ps.setInt(1, studentId);
                    ps.setInt(2, subjectId);
                    ps.setString(3, examType);

                    try (
                        ResultSet rs =
                            ps.executeQuery()
                    ) {

                        if (rs.next()) {

                            existingId =
                                rs.getInt("id");
                        }
                    }
                }


                // -------------------------------------------------
                // SAVE / UPDATE MARKS
                // -------------------------------------------------

                boolean databaseSuccess = false;

                String action = "";


                if (existingId != -1) {

                    String updateSql =
                        "UPDATE marks "
                        + "SET marks = ? "
                        + "WHERE id = ?";

                    try (
                        PreparedStatement ps =
                            con.prepareStatement(updateSql)
                    ) {

                        ps.setDouble(1, marks);
                        ps.setInt(2, existingId);

                        int updated =
                            ps.executeUpdate();

                        if (updated > 0) {

                            databaseSuccess = true;
                            action = "update";
                        }
                    }

                } else {

                    String insertSql =
                        "INSERT INTO marks "
                        + "(student_id, subject_id, exam_type, marks) "
                        + "VALUES (?, ?, ?, ?)";

                    try (
                        PreparedStatement ps =
                            con.prepareStatement(insertSql)
                    ) {

                        ps.setInt(1, studentId);
                        ps.setInt(2, subjectId);
                        ps.setString(3, examType);
                        ps.setDouble(4, marks);

                        int inserted =
                            ps.executeUpdate();

                        if (inserted > 0) {

                            databaseSuccess = true;
                            action = "insert";
                        }
                    }
                }


                // -------------------------------------------------
                // DATABASE FAILED
                // -------------------------------------------------

                if (!databaseSuccess) {

                    out.print(
                        "{\"success\":false,"
                        + "\"message\":\"Marks could not be saved.\"}"
                    );

                    return;
                }


                // -------------------------------------------------
                // SEND EMAIL AFTER MYSQL SUCCESS
                // -------------------------------------------------

                boolean emailSent = false;

                if (!studentEmail.trim().isEmpty()) {

                    System.out.println();
                    System.out.println(
                        "=========================================="
                    );

                    System.out.println(
                        "SMARTATTEND MARKS NOTIFICATION"
                    );

                    System.out.println(
                        "Student : " + studentName
                    );

                    System.out.println(
                        "Email   : " + studentEmail
                    );

                    System.out.println(
                        "Subject : " + subjectName
                    );

                    System.out.println(
                        "Marks   : " + marks
                    );

                    System.out.println(
                        "Action  : " + action
                    );

                    System.out.println(
                        "=========================================="
                    );


                    emailSent =
                        EmailJSService.sendMarksNotification(
                            studentEmail,
                            studentName,
                            subjectName,
                            String.valueOf(marks)
                        );

                } else {

                    System.out.println(
                        "WARNING: Student email is empty."
                    );
                }


                // -------------------------------------------------
                // FINAL RESPONSE
                // -------------------------------------------------

                String message;

                if ("update".equals(action)) {

                    message =
                        "Marks updated successfully.";

                } else {

                    message =
                        "Marks saved successfully.";
                }


                out.print(
                    "{"
                    + "\"success\":true,"
                    + "\"action\":\""
                    + escapeJson(action)
                    + "\","
                    + "\"message\":\""
                    + escapeJson(message)
                    + "\","
                    + "\"emailSent\":"
                    + emailSent
                    + ","
                    + "\"studentEmail\":\""
                    + escapeJson(studentEmail)
                    + "\""
                    + "}"
                );
            }

        } catch (NumberFormatException e) {

            out.print(
                "{\"success\":false,"
                + "\"message\":\"Invalid student, subject or marks value.\"}"
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


    // =========================================================
    // SIMPLE JSON VALUE READER
    // =========================================================

    private String getJsonValue(
            String json,
            String key) {

        if (json == null || key == null) {
            return null;
        }

        String search =
            "\"" + key + "\"";

        int keyIndex =
            json.indexOf(search);

        if (keyIndex == -1) {
            return null;
        }

        int colonIndex =
            json.indexOf(
                ":",
                keyIndex + search.length()
            );

        if (colonIndex == -1) {
            return null;
        }

        int start =
            colonIndex + 1;

        while (start < json.length()
                && Character.isWhitespace(
                    json.charAt(start)
                )) {

            start++;
        }

        if (start >= json.length()) {
            return null;
        }

        if (json.charAt(start) == '"') {

            start++;

            StringBuilder value =
                new StringBuilder();

            boolean escaped = false;

            for (
                int i = start;
                i < json.length();
                i++
            ) {

                char c =
                    json.charAt(i);

                if (escaped) {

                    value.append(c);
                    escaped = false;

                } else if (c == '\\') {

                    escaped = true;

                } else if (c == '"') {

                    return value.toString();

                } else {

                    value.append(c);
                }
            }

            return null;
        }

        int end = start;

        while (end < json.length()) {

            char c =
                json.charAt(end);

            if (c == ',' || c == '}') {
                break;
            }

            end++;
        }

        return json
            .substring(start, end)
            .trim();
    }


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String escapeJson(
            String value) {

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
