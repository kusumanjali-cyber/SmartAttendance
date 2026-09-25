import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_HOST =
        getEnv("DB_HOST", "localhost");

private static final String DB_PORT =
        getEnv("DB_PORT", "3306");

private static final String DB_NAME =
        getEnv("DB_NAME", "smartattendance");

private static final String DB_USER =
        getEnv("DB_USER", "root");

private static final String DB_PASSWORD =
        getEnv("DB_PASSWORD", "");

private static final String DB_URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?sslMode="
        + (DB_HOST.equals("localhost") ? "DISABLED" : "REQUIRED")
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=UTC";

private static String getEnv(String name, String defaultValue) {
    String value = System.getenv(name);

    if (value == null || value.trim().isEmpty()) {
        return defaultValue;
    }

    return value.trim();
}

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        String rollNumber = request.getParameter("rollNumber");
        String department = request.getParameter("department");
        String section = request.getParameter("section");
        String yearValue = request.getParameter("year");
        String subject = request.getParameter("subject");

        // =====================================================
        // BASIC VALIDATION
        // =====================================================

        if (name == null ||
            email == null ||
            password == null ||
            role == null ||
            name.trim().isEmpty() ||
            email.trim().isEmpty() ||
            password.trim().isEmpty() ||
            role.trim().isEmpty()) {

            response.sendRedirect("register.html?error=missing");
            return;
        }

        name = name.trim();
        email = email.trim().toLowerCase();
        password = password.trim();
        role = role.trim().toUpperCase();

        // =====================================================
        // ROLE VALIDATION
        // =====================================================

        if (!role.equals("STUDENT") &&
            !role.equals("FACULTY") &&
            !role.equals("ADMIN")) {

            response.sendRedirect("register.html?error=invalidrole");
            return;
        }

        // =====================================================
        // STUDENT DETAILS
        // =====================================================

        int year = 1;

        if ("STUDENT".equals(role)) {

            if (rollNumber == null ||
                department == null ||
                section == null ||
                rollNumber.trim().isEmpty() ||
                department.trim().isEmpty() ||
                section.trim().isEmpty()) {

                response.sendRedirect("register.html?error=missing");
                return;
            }

            rollNumber = rollNumber.trim();
            department = department.trim();
            section = section.trim();

            if (yearValue != null &&
                !yearValue.trim().isEmpty()) {

                try {

                    year = Integer.parseInt(
                            yearValue.trim()
                    );

                } catch (NumberFormatException e) {

                    response.sendRedirect(
                            "register.html?error=invalidyear"
                    );

                    return;
                }

                if (year < 1 || year > 4) {

                    response.sendRedirect(
                            "register.html?error=invalidyear"
                    );

                    return;
                }
            }
        }
        if ("FACULTY".equals(role)) {

    if (subject == null || subject.trim().isEmpty()) {

        response.sendRedirect(
                "register.html?error=missing"
        );

        return;
    }

    subject = subject.trim();
}

        Connection con = null;
        PreparedStatement insertUser = null;
        PreparedStatement insertStudent = null;
        ResultSet generatedKeys = null;

        try {

            // =================================================
            // LOAD MYSQL DRIVER
            // =================================================

            Class.forName("com.mysql.cj.jdbc.Driver");

            // =================================================
            // DATABASE CONNECTION
            // =================================================

            con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );

            con.setAutoCommit(false);

            // =================================================
            // INSERT USER
            // =================================================

            String insertUserSQL =
        "INSERT INTO users "
        + "(name, username, password, role, subject) "
        + "VALUES (?, ?, ?, ?, ?)";

            insertUser = con.prepareStatement(
                    insertUserSQL,
                    Statement.RETURN_GENERATED_KEYS
            );

            insertUser.setString(1, name);
            insertUser.setString(2, email);
            insertUser.setString(3, password);
            insertUser.setString(4, role);
            insertUser.setString(
        5,
        "FACULTY".equals(role) ? subject : null
);

            int userCount = insertUser.executeUpdate();

            if (userCount != 1) {

                con.rollback();

                response.sendRedirect(
                        "register.html?error=failed"
                );

                return;
            }

            // =================================================
            // GET GENERATED USER ID
            // =================================================

            generatedKeys = insertUser.getGeneratedKeys();

            int userId = 0;

            if (generatedKeys.next()) {

                userId = generatedKeys.getInt(1);
            }

            if (userId <= 0) {

                con.rollback();

                response.sendRedirect(
                        "register.html?error=failed"
                );

                return;
            }

            // =================================================
            // INSERT STUDENT DETAILS
            // ONLY FOR STUDENT
            // =================================================

            if ("STUDENT".equals(role)) {

                String studentSQL =
                        "INSERT INTO students "
                        + "(student_name, roll_number, email, "
                        + "department, section, year) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

                insertStudent =
                        con.prepareStatement(studentSQL);

                insertStudent.setString(
                        1,
                        name
                );

                insertStudent.setString(
                        2,
                        rollNumber
                );

                insertStudent.setString(
                        3,
                        email
                );

                insertStudent.setString(
                        4,
                        department
                );

                insertStudent.setString(
                        5,
                        section
                );

                insertStudent.setInt(
                        6,
                        year
                );

                int studentCount =
                        insertStudent.executeUpdate();

                if (studentCount != 1) {

                    con.rollback();

                    response.sendRedirect(
                            "register.html?error=failed"
                    );

                    return;
                }
            }

            // =================================================
            // COMMIT
            // =================================================

            con.commit();
         // Send registration email using EmailJS
if ("STUDENT".equals(role) || "FACULTY".equals(role)) {

    boolean emailSent =
            EmailJSService.sendRegistrationEmail(
                    email,
                    name,
                    role
            );

    if (emailSent) {
        System.out.println(
                "Registration email sent successfully."
        );
    } else {
        System.out.println(
                "Registration email failed."
        );
    }
} 


            // =================================================
            // CREATE SESSION
            // =================================================

            HttpSession session =
                    request.getSession(true);

            session.setAttribute(
                    "userId",
                    userId
            );

            session.setAttribute(
                    "name",
                    name
            );

            session.setAttribute(
                    "email",
                    email
            );

            session.setAttribute(
                    "username",
                    email
            );

            session.setAttribute(
                    "role",
                    role
            );

            session.setAttribute(
                    "passwordVerified",
                    true
            );

            // =================================================
            // DIRECT DASHBOARD
            // =================================================

            if ("STUDENT".equals(role)) {

                response.sendRedirect(
                        "student.html"
                );

                return;
            }

            if ("FACULTY".equals(role)) {

                response.sendRedirect(
                        "faculty.html"
                );

                return;
            }

            if ("ADMIN".equals(role)) {

                response.sendRedirect(
                        "admin.html"
                );

                return;
            }

            // =================================================
            // FALLBACK
            // =================================================

            response.sendRedirect(
                    "login.html"
            );

        } catch (Exception e) {

            e.printStackTrace();

            if (con != null) {

                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }

            // =================================================
            // SHOW REAL DATABASE ERROR
            // =================================================

            System.out.println(
                    "========== REGISTER ERROR =========="
            );

            e.printStackTrace();

            System.out.println(
                    "===================================="
            );

            response.setContentType(
                    "text/html;charset=UTF-8"
            );

            response.getWriter().println(
                    "<h2>Registration Error</h2>"
            );

            response.getWriter().println(
                    "<pre>"
            );

            response.getWriter().println(
                    e.getClass().getName()
            );

            response.getWriter().println(
                    ": " + e.getMessage()
            );

            response.getWriter().println(
                    "</pre>"
            );

        } finally {

            // =================================================
            // CLOSE GENERATED KEYS
            // =================================================

            try {

                if (generatedKeys != null) {
                    generatedKeys.close();
                }

            } catch (Exception ignored) {
            }

            // =================================================
            // CLOSE STUDENT STATEMENT
            // =================================================

            try {

                if (insertStudent != null) {
                    insertStudent.close();
                }

            } catch (Exception ignored) {
            }

            // =================================================
            // CLOSE USER STATEMENT
            // =================================================

            try {

                if (insertUser != null) {
                    insertUser.close();
                }

            } catch (Exception ignored) {
            }

            // =================================================
            // CLOSE CONNECTION
            // =================================================

            try {

                if (con != null) {
                    con.close();
                }

            } catch (Exception ignored) {
            }
        }
    }
}