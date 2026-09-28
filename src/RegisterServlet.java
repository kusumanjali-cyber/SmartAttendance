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

    // =========================================================
    // GET REQUIRED ENVIRONMENT VARIABLE
    // =========================================================

    private String getRequiredEnv(String key) {

        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    // =========================================================
    // POST
    // =========================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // =====================================================
        // DATABASE ENVIRONMENT VARIABLES
        // =====================================================

        String dbUrl = "jdbc:mysql://" + getRequiredEnv("DB_URL");
        String dbUser = getRequiredEnv("DB_USER");
        String dbPassword = getRequiredEnv("DB_PASSWORD");

        // =====================================================
        // FORM VALUES
        // =====================================================

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        String rollNumber = request.getParameter("rollNumber");
        String department = request.getParameter("department");
        String section = request.getParameter("section");
        String yearValue = request.getParameter("year");

        // =====================================================
        // BASIC VALIDATION
        // =====================================================

        if (name == null
                || email == null
                || password == null
                || role == null
                || name.trim().isEmpty()
                || email.trim().isEmpty()
                || password.trim().isEmpty()
                || role.trim().isEmpty()) {

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

        if (!role.equals("STUDENT")
                && !role.equals("FACULTY")
                && !role.equals("ADMIN")) {

            response.sendRedirect("register.html?error=invalidrole");
            return;
        }

        // =====================================================
        // STUDENT DETAILS
        // =====================================================

        int year = 1;

        if ("STUDENT".equals(role)) {

            if (rollNumber == null
                    || department == null
                    || section == null
                    || rollNumber.trim().isEmpty()
                    || department.trim().isEmpty()
                    || section.trim().isEmpty()) {

                response.sendRedirect("register.html?error=missing");
                return;
            }

            rollNumber = rollNumber.trim();
            department = department.trim();
            section = section.trim();

            if (yearValue != null
                    && !yearValue.trim().isEmpty()) {

                try {

                    year = Integer.parseInt(yearValue.trim());

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

        // =====================================================
        // DATABASE CONNECTION
        // =====================================================

        Connection con = null;
        PreparedStatement insertUser = null;
        PreparedStatement insertStudent = null;
        ResultSet generatedKeys = null;

        try {

            // =================================================
            // CHECK DATABASE VARIABLES
            // =================================================

            if (dbUrl == null
                    || dbUser == null
                    || dbPassword == null) {

                throw new ServletException(
                        "Database environment variables are missing."
                );
            }

            // =================================================
            // SAFE LOG
            // =================================================

            System.out.println("====================================");
            System.out.println("DATABASE CONNECTION START");
            System.out.println("DB URL  : " + dbUrl);
            System.out.println("DB USER : " + dbUser);
            System.out.println("====================================");

            // =================================================
            // MYSQL DRIVER
            // =================================================

            Class.forName("com.mysql.cj.jdbc.Driver");

            // =================================================
            // CONNECT
            // =================================================

            con = DriverManager.getConnection(
                    dbUrl,
                    dbUser,
                    dbPassword
            );

            System.out.println(
                    "DATABASE CONNECTION SUCCESS"
            );

            con.setAutoCommit(false);

            // =================================================
            // INSERT USER
            // =================================================

            String insertUserSQL =
                    "INSERT INTO users "
                    + "(name, username, password, role) "
                    + "VALUES (?, ?, ?, ?)";

            insertUser = con.prepareStatement(
                    insertUserSQL,
                    Statement.RETURN_GENERATED_KEYS
            );

            insertUser.setString(1, name);
            insertUser.setString(2, email);
            insertUser.setString(3, password);
            insertUser.setString(4, role);

            int userCount =
                    insertUser.executeUpdate();

            if (userCount != 1) {

                con.rollback();

                response.sendRedirect(
                        "register.html?error=failed"
                );

                return;
            }

            // =================================================
            // GET USER ID
            // =================================================

            generatedKeys =
                    insertUser.getGeneratedKeys();

            int userId = 0;

            if (generatedKeys.next()) {

                userId =
                        generatedKeys.getInt(1);
            }

            if (userId <= 0) {

                con.rollback();

                response.sendRedirect(
                        "register.html?error=failed"
                );

                return;
            }

            // =================================================
            // INSERT STUDENT
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

            System.out.println("====================================");
            System.out.println("REGISTRATION SUCCESS");
            System.out.println("User ID : " + userId);
            System.out.println("Name    : " + name);
            System.out.println("Email   : " + email);
            System.out.println("Role    : " + role);
            System.out.println("====================================");

            // =================================================
            // SESSION
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
            // ROLE DASHBOARD
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

            response.sendRedirect("login.html");

        } catch (Exception e) {

            e.printStackTrace();

            if (con != null) {

                try {
                    con.rollback();
                } catch (Exception ignored) {
                }
            }

            System.out.println(
                    "========== REGISTER ERROR =========="
            );

            System.out.println(
                    "ERROR TYPE : "
                    + e.getClass().getName()
            );

            System.out.println(
                    "ERROR MSG  : "
                    + e.getMessage()
            );

            System.out.println(
                    "DB USER    : "
                    + dbUser
            );

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

            try {

                if (generatedKeys != null) {
                    generatedKeys.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (insertStudent != null) {
                    insertStudent.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (insertUser != null) {
                    insertUser.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (con != null) {
                    con.close();
                }

            } catch (Exception ignored) {
            }
        }
    }
}