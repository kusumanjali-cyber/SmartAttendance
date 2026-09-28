import java.io.IOException;
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

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;


    // =====================================================
    // GET ENVIRONMENT VARIABLE
    // =====================================================

    private String getEnv(String key, String fallback) {

        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }


    // =====================================================
    // DATABASE URL
    // =====================================================

    private String getDatabaseUrl() {

        String configuredUrl =
                System.getenv("DB_URL");

        /*
         * If DB_URL is already a complete JDBC URL,
         * use it directly.
         */

        if (configuredUrl != null &&
            configuredUrl.trim().startsWith("jdbc:mysql://")) {

            return configuredUrl.trim();
        }


        /*
         * Render setup:
         *
         * DB_URL      = database hostname
         * DB_PORT     = database port
         * DB_NAME     = database name
         *
         * Example:
         *
         * jdbc:mysql://hostname:21100/defaultdb
         */

        String host =
                getEnv(
                        "DB_HOST",
                        configuredUrl
                );

        String port =
                getEnv(
                        "DB_PORT",
                        "3306"
                );

        String database =
                getEnv(
                        "DB_NAME",
                        "smartattendance"
                );


        return "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + database
                + "?useSSL=true"
                + "&requireSSL=true"
                + "&verifyServerCertificate=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";
    }


    // =====================================================
    // POST LOGIN
    // =====================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");


        // =================================================
        // FORM VALUES
        // =================================================

        String email =
                request.getParameter("username");

        String password =
                request.getParameter("password");


        // Support email field also

        if (email == null ||
            email.trim().isEmpty()) {

            email =
                    request.getParameter("email");
        }


        // =================================================
        // CHECK EMPTY FIELDS
        // =================================================

        if (email == null ||
            password == null ||
            email.trim().isEmpty() ||
            password.trim().isEmpty()) {

            response.sendRedirect(
                    "login.html?error=missing"
            );

            return;
        }


        email =
                email.trim().toLowerCase();

        password =
                password.trim();


        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;


        try {

            // =================================================
            // DATABASE CONFIGURATION
            // =================================================

            String dbUrl =
                    getDatabaseUrl();

            String dbUser =
                    getEnv(
                            "DB_USER",
                            "root"
                    );

            String dbPassword =
                    getEnv(
                            "DB_PASSWORD",
                            "root123"
                    );


            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "SMARTATTEND LOGIN - DATABASE CONNECTION"
            );

            System.out.println(
                    "DB URL  : " + dbUrl
            );

            System.out.println(
                    "DB USER : " + dbUser
            );

            System.out.println(
                    "=========================================="
            );


            // =================================================
            // LOAD MYSQL DRIVER
            // =================================================

            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );


            // =================================================
            // CONNECT DATABASE
            // =================================================

            connection =
                    DriverManager.getConnection(
                            dbUrl,
                            dbUser,
                            dbPassword
                    );


            System.out.println(
                    "LOGIN DATABASE CONNECTION SUCCESS"
            );


            // =================================================
            // LOGIN QUERY
            // =================================================

            String sql =
                    "SELECT id, name, username, role, subject "
                    + "FROM users "
                    + "WHERE LOWER(TRIM(username)) = ? "
                    + "AND TRIM(password) = ?";


            statement =
                    connection.prepareStatement(sql);


            statement.setString(
                    1,
                    email
            );

            statement.setString(
                    2,
                    password
            );


            resultSet =
                    statement.executeQuery();


            // =================================================
            // LOGIN FAILED
            // =================================================

            if (!resultSet.next()) {

                System.out.println(
                        "LOGIN FAILED: "
                        + email
                );


                response.sendRedirect(
                        "login.html?error=invalid"
                );

                return;
            }


            // =================================================
            // GET USER DETAILS
            // =================================================

            int userId =
                    resultSet.getInt("id");


            String name =
                    resultSet.getString("name");


            String username =
                    resultSet.getString("username");


            String role =
                    resultSet.getString("role");


            String subject =
                    resultSet.getString("subject");


            if (name == null) {
                name = "";
            }


            if (username == null) {
                username = "";
            }


            if (role == null) {
                role = "";
            }


            if (subject == null) {
                subject = "";
            }


            role =
                    role.trim().toUpperCase();


            // =================================================
            // LOGIN SUCCESS
            // =================================================

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "LOGIN SUCCESS"
            );

            System.out.println(
                    "User ID : " + userId
            );

            System.out.println(
                    "Name    : " + name
            );

            System.out.println(
                    "Email   : " + username
            );

            System.out.println(
                    "Role    : " + role
            );

            System.out.println(
                    "=========================================="
            );


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
                    username
            );


            session.setAttribute(
                    "username",
                    username
            );


            session.setAttribute(
                    "role",
                    role
            );


            session.setAttribute(
                    "subject",
                    subject
            );


            session.setAttribute(
                    "passwordVerified",
                    true
            );


            // =================================================
            // ROLE BASED REDIRECT
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
            // UNKNOWN ROLE
            // =================================================

            session.invalidate();


            response.sendRedirect(
                    "login.html?error=invalidrole"
            );


        } catch (Exception e) {

            // =================================================
            // LOGIN ERROR
            // =================================================

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "LOGIN DATABASE ERROR"
            );

            System.out.println(
                    "ERROR TYPE: "
                    + e.getClass().getName()
            );

            System.out.println(
                    "ERROR MESSAGE: "
                    + e.getMessage()
            );

            System.out.println(
                    "=========================================="
            );


            e.printStackTrace();


            response.sendRedirect(
                    "login.html?error=server"
            );


        } finally {


            // =================================================
            // CLOSE RESULT SET
            // =================================================

            try {

                if (resultSet != null) {
                    resultSet.close();
                }

            } catch (Exception ignored) {
            }


            // =================================================
            // CLOSE STATEMENT
            // =================================================

            try {

                if (statement != null) {
                    statement.close();
                }

            } catch (Exception ignored) {
            }


            // =================================================
            // CLOSE CONNECTION
            // =================================================

            try {

                if (connection != null) {
                    connection.close();
                }

            } catch (Exception ignored) {
            }
        }
    }


    // =====================================================
    // GET
    // =====================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(
                "login.html"
        );
    }
}