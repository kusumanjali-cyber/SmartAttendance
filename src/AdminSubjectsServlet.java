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

@WebServlet("/AdminSubjectsServlet")
public class AdminSubjectsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/smartattendance"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";

    // =========================
    // GET - LOAD SUBJECTS
    // =========================
    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        StringBuilder json = new StringBuilder();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String sql =
                    "SELECT id, subject_name " +
                    "FROM subjects " +
                    "ORDER BY id";

            try (Connection con =
                         DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                json.append("{\"success\":true,\"subjects\":[");

                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{")
                            .append("\"id\":")
                            .append(rs.getInt("id"))
                            .append(",")

                            .append("\"subjectName\":\"")
                            .append(escapeJson(rs.getString("subject_name")))
                            .append("\"")

                            .append("}");
                }

                json.append("]}");
            }

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

    // =========================
    // POST - ADD / UPDATE / DELETE
    // =========================
    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        if (isEmpty(action)) {

            out.print(
                    "{\"success\":false,\"message\":\"Action is required\"}"
            );

            return;
        }

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            if ("add".equalsIgnoreCase(action)) {

                addSubject(request, out);

            } else if ("update".equalsIgnoreCase(action)) {

                updateSubject(request, out);

            } else if ("delete".equalsIgnoreCase(action)) {

                deleteSubject(request, out);

            } else {

                out.print(
                        "{\"success\":false,\"message\":\"Invalid action\"}"
                );
            }

        } catch (NumberFormatException e) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid number\"}"
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

    // =========================
    // ADD SUBJECT
    // =========================
    private void addSubject(HttpServletRequest request,
                            PrintWriter out)
            throws Exception {

        String subjectName = request.getParameter("subjectName");

        if (isEmpty(subjectName)) {

            out.print(
                    "{\"success\":false,\"message\":\"Please enter subject name\"}"
            );

            return;
        }

        subjectName = subjectName.trim();

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String checkSql =
                    "SELECT id FROM subjects " +
                    "WHERE LOWER(TRIM(subject_name)) = LOWER(TRIM(?))";

            try (PreparedStatement check =
                         con.prepareStatement(checkSql)) {

                check.setString(1, subjectName);

                try (ResultSet rs = check.executeQuery()) {

                    if (rs.next()) {

                        out.print(
                                "{\"success\":false,\"message\":\"Subject already exists\"}"
                        );

                        return;
                    }
                }
            }

            String insertSql =
                    "INSERT INTO subjects (subject_name) VALUES (?)";

            try (PreparedStatement ps =
                         con.prepareStatement(insertSql)) {

                ps.setString(1, subjectName);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    out.print(
                            "{\"success\":true,\"message\":\"Subject added successfully\"}"
                    );

                } else {

                    out.print(
                            "{\"success\":false,\"message\":\"Subject was not added\"}"
                    );
                }
            }
        }
    }

    // =========================
    // UPDATE SUBJECT
    // =========================
    private void updateSubject(HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        String idText = request.getParameter("id");
        String subjectName = request.getParameter("subjectName");

        if (isEmpty(idText) || isEmpty(subjectName)) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid subject details\"}"
            );

            return;
        }

        int id;

        try {

            id = Integer.parseInt(idText);

        } catch (NumberFormatException e) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid subject ID\"}"
            );

            return;
        }

        if (id <= 0) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid subject ID\"}"
            );

            return;
        }

        subjectName = subjectName.trim();

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String checkSql =
                    "SELECT id FROM subjects " +
                    "WHERE LOWER(TRIM(subject_name)) = LOWER(TRIM(?)) " +
                    "AND id <> ?";

            try (PreparedStatement check =
                         con.prepareStatement(checkSql)) {

                check.setString(1, subjectName);
                check.setInt(2, id);

                try (ResultSet rs = check.executeQuery()) {

                    if (rs.next()) {

                        out.print(
                                "{\"success\":false,\"message\":\"Another subject with this name already exists\"}"
                        );

                        return;
                    }
                }
            }

            String updateSql =
                    "UPDATE subjects " +
                    "SET subject_name = ? " +
                    "WHERE id = ?";

            try (PreparedStatement ps =
                         con.prepareStatement(updateSql)) {

                ps.setString(1, subjectName);
                ps.setInt(2, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    out.print(
                            "{\"success\":true,\"message\":\"Subject updated successfully\"}"
                    );

                } else {

                    out.print(
                            "{\"success\":false,\"message\":\"Subject not found\"}"
                    );
                }
            }
        }
    }

    // =========================
    // DELETE SUBJECT
    // =========================
    private void deleteSubject(HttpServletRequest request,
                               PrintWriter out)
            throws Exception {

        String idText = request.getParameter("id");

        if (isEmpty(idText)) {

            out.print(
                    "{\"success\":false,\"message\":\"Subject ID is required\"}"
            );

            return;
        }

        int id;

        try {

            id = Integer.parseInt(idText);

        } catch (NumberFormatException e) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid subject ID\"}"
            );

            return;
        }

        if (id <= 0) {

            out.print(
                    "{\"success\":false,\"message\":\"Invalid subject ID\"}"
            );

            return;
        }

        try (Connection con =
                     DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String deleteSql =
                    "DELETE FROM subjects WHERE id = ?";

            try (PreparedStatement ps =
                         con.prepareStatement(deleteSql)) {

                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    out.print(
                            "{\"success\":true,\"message\":\"Subject deleted successfully\"}"
                    );

                } else {

                    out.print(
                            "{\"success\":false,\"message\":\"Subject not found\"}"
                    );
                }
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {

            out.print(
                    "{\"success\":false,\"message\":\"This subject is already used in attendance records. Please rename it instead of deleting it.\"}"
            );
        }
    }

    // =========================
    // EMPTY CHECK
    // =========================
    private boolean isEmpty(String value) {

        return value == null || value.trim().isEmpty();
    }

    // =========================
    // JSON ESCAPE
    // =========================
    private String escapeJson(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}