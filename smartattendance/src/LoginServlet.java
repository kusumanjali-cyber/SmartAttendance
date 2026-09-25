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

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        // Support email field also
        if (email == null || email.trim().isEmpty()) {
            email = request.getParameter("email");
        }

        if (email == null || password == null
                || email.trim().isEmpty()
                || password.trim().isEmpty()) {

            response.sendRedirect("login.html?error=missing");
            return;
        }

        email = email.trim().toLowerCase();
        password = password.trim();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );

            String sql =
        "SELECT id, name, username, role, subject "
        + "FROM users "
        + "WHERE LOWER(TRIM(username)) = ? "
        + "AND TRIM(password) = ?";

            statement = connection.prepareStatement(sql);

            statement.setString(1, email);
            statement.setString(2, password);

            resultSet = statement.executeQuery();

            if (!resultSet.next()) {

                System.out.println(
                        "LOGIN FAILED: " + email
                );

                response.sendRedirect(
                        "login.html?error=invalid"
                );

                return;
            }

            int userId = resultSet.getInt("id");

            String name = resultSet.getString("name");
            String username =
                    resultSet.getString("username");

            String role =
                    resultSet.getString("role");
        String subject =
        resultSet.getString("subject");

            if (role == null) {
                role = "";
            }

            role = role.trim().toUpperCase();

            System.out.println(
                    "========== LOGIN SUCCESS =========="
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
                    "==================================="
            );

            // Create session
            HttpSession session =
                    request.getSession(true);

            session.setAttribute(
                    "userId",
                    userId
            );

            session.setAttribute(
                    "name",
                    name == null ? "" : name
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
        subject == null ? "" : subject
);
            session.setAttribute(
                    "passwordVerified",
                    true
            );

            // ==============================
            // ROLE BASED REDIRECT
            // ==============================

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

            // Unknown role
            session.invalidate();

            response.sendRedirect(
                    "login.html?error=invalidrole"
            );

        } catch (Exception e) {

            System.out.println(
                    "========== LOGIN ERROR =========="
            );

            e.printStackTrace();

            System.out.println(
                    "================================="
            );

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

        response.sendRedirect("login.html");
    }
}