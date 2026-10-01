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

    private String getRequiredEnv(String key) {

        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("email");
        }

        if (email == null ||
            password == null ||
            email.trim().isEmpty() ||
            password.trim().isEmpty()) {

            response.sendRedirect("login.html?error=missing");
            return;
        }

        email = email.trim().toLowerCase();
        password = password.trim();

        String dbUrl = getRequiredEnv("DB_URL");
        String dbUser = getRequiredEnv("DB_USER");
        String dbPassword = getRequiredEnv("DB_PASSWORD");

        if (dbUrl == null ||
            dbUser == null ||
            dbPassword == null) {

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "LOGIN ERROR: DATABASE ENVIRONMENT VARIABLES MISSING"
            );

            System.out.println(
                    "DB_URL present      : " + (dbUrl != null)
            );

            System.out.println(
                    "DB_USER present     : " + (dbUser != null)
            );

            System.out.println(
                    "DB_PASSWORD present : " + (dbPassword != null)
            );

            System.out.println(
                    "=========================================="
            );

            response.sendRedirect("login.html?error=server");
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {

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
                    "LOGIN EMAIL : " + email
            );

            System.out.println(
                    "=========================================="
            );

            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );

            connection =
                    DriverManager.getConnection(
                            dbUrl,
                            dbUser,
                            dbPassword
                    );

            System.out.println(
                    "LOGIN DATABASE CONNECTION SUCCESS"
            );

            System.out.println(
                    "LOGIN DEBUG EMAIL = [" + email + "]"
            );

            System.out.println(
                    "LOGIN DEBUG PASSWORD LENGTH = "
                    + password.length()
            );

            /*
             * First find the user by username.
             * The users table stores the email in the username column.
             */
            String sql =
                    "SELECT id, name, username, password, role, subject "
                    + "FROM users "
                    + "WHERE LOWER(TRIM(username)) = ?";

            statement =
                    connection.prepareStatement(sql);

            statement.setString(1, email);

            resultSet =
                    statement.executeQuery();

            if (!resultSet.next()) {

                System.out.println(
                        "LOGIN DEBUG: USERNAME NOT FOUND"
                );

                System.out.println(
                        "LOGIN EMAIL: [" + email + "]"
                );

                response.sendRedirect(
                        "login.html?error=invalid"
                );

                return;
            }

            /*
             * Username exists.
             * Now compare the submitted password with the stored password.
             */
            String storedPassword =
                    resultSet.getString("password");

            if (storedPassword == null) {

                System.out.println(
                        "LOGIN DEBUG: STORED PASSWORD IS NULL"
                );

                response.sendRedirect(
                        "login.html?error=invalid"
                );

                return;
            }

            if (!storedPassword.trim().equals(password)) {

                System.out.println(
                        "LOGIN DEBUG: USERNAME FOUND "
                        + "BUT PASSWORD DOES NOT MATCH"
                );

                System.out.println(
                        "LOGIN DEBUG STORED PASSWORD LENGTH = "
                        + storedPassword.length()
                );

                response.sendRedirect(
                        "login.html?error=invalid"
                );

                return;
            }

            System.out.println(
                    "LOGIN DEBUG: USERNAME AND PASSWORD MATCH"
            );

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

            session.invalidate();

            response.sendRedirect(
                    "login.html?error=invalidrole"
            );

        } catch (Exception e) {

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

            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

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