import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
private static final String DB_PORT = getEnv("DB_PORT", "3306");
private static final String DB_NAME = getEnv("DB_NAME", "smartattendance");
private static final String DB_USER = getEnv("DB_USER", "root");
private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "");

private static String getEnv(String key, String defaultValue) {
    String value = System.getenv(key);
    return (value == null || value.trim().isEmpty()) ? defaultValue : value;
}

    // ==============================
    // GMAIL SETTINGS
    // ==============================

    private static final String SENDER_EMAIL =
        "kusumanjaligadupudi@gmail.com";

// Gmail 16-character App Password
// Set this in Windows as SMART_ATTENDANCE_GMAIL_APP_PASSWORD.
// Do NOT hard-code the password in this source file.
private static final String APP_PASSWORD =
        System.getenv("SMART_ATTENDANCE_GMAIL_APP_PASSWORD");
    @Override
    protected void doPost(
            HttpServletRequest request,
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

        // ==============================
        // BASIC VALIDATION
        // ==============================

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

        // ==============================
        // ROLE VALIDATION
        // ==============================

        if (!role.equals("STUDENT")
                && !role.equals("FACULTY")
                && !role.equals("ADMIN")) {

            response.sendRedirect("register.html?error=invalidrole");
            return;
        }

        // ==============================
        // STUDENT DETAILS
        // ==============================

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

                    response.sendRedirect("register.html?error=invalidyear");
                    return;
                }

                if (year < 1 || year > 4) {

                    response.sendRedirect("register.html?error=invalidyear");
                    return;
                }
            }
        }

        Connection con = null;
        PreparedStatement insertUser = null;
        PreparedStatement insertStudent = null;
        ResultSet generatedKeys = null;

        try {

            // ==============================
            // MYSQL DRIVER
            // ==============================

            Class.forName("com.mysql.cj.jdbc.Driver");

            // ==============================
            // DATABASE CONNECTION
            // ==============================

            con = DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );

            con.setAutoCommit(false);

            // ==============================
            // INSERT USER
            // ==============================

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

            int userCount = insertUser.executeUpdate();

            if (userCount != 1) {

                con.rollback();

                response.sendRedirect("register.html?error=failed");
                return;
            }

            // ==============================
            // GET USER ID
            // ==============================

            generatedKeys = insertUser.getGeneratedKeys();

            int userId = 0;

            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1);
            }

            if (userId <= 0) {

                con.rollback();

                response.sendRedirect("register.html?error=failed");
                return;
            }

            // ==============================
            // INSERT STUDENT DETAILS
            // ==============================

            if ("STUDENT".equals(role)) {

                String studentSQL =
                        "INSERT INTO students "
                        + "(student_name, roll_number, email, "
                        + "department, section, year) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

                insertStudent = con.prepareStatement(studentSQL);

                insertStudent.setString(1, name);
                insertStudent.setString(2, rollNumber);
                insertStudent.setString(3, email);
                insertStudent.setString(4, department);
                insertStudent.setString(5, section);
                insertStudent.setInt(6, year);

                int studentCount =
                        insertStudent.executeUpdate();

                if (studentCount != 1) {

                    con.rollback();

                    response.sendRedirect("register.html?error=failed");
                    return;
                }
            }

            // ==============================
            // COMMIT
            // ==============================

            con.commit();

            System.out.println("====================================");
            System.out.println("REGISTRATION SUCCESS");
            System.out.println("User ID : " + userId);
            System.out.println("Name    : " + name);
            System.out.println("Email   : " + email);
            System.out.println("Role    : " + role);
            System.out.println("====================================");

            // ==============================
            // SEND WELCOME EMAIL
            // ==============================

            if ("STUDENT".equals(role)
                    || "FACULTY".equals(role)) {

                sendWelcomeEmail(
                        email,
                        name,
                        role
                );
            }

            // ==============================
            // SESSION
            // ==============================

            HttpSession session =
                    request.getSession(true);

            session.setAttribute("userId", userId);
            session.setAttribute("name", name);
            session.setAttribute("email", email);
            session.setAttribute("username", email);
            session.setAttribute("role", role);
            session.setAttribute("passwordVerified", true);

            // ==============================
            // DASHBOARD
            // ==============================

            if ("STUDENT".equals(role)) {

                response.sendRedirect("student.html");
                return;
            }

            if ("FACULTY".equals(role)) {

                response.sendRedirect("faculty.html");
                return;
            }

            if ("ADMIN".equals(role)) {

                response.sendRedirect("admin.html");
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

            System.out.println("========== REGISTER ERROR ==========");
            System.out.println(e.getClass().getName());
            System.out.println(e.getMessage());
            System.out.println("====================================");

            response.setContentType("text/html;charset=UTF-8");

            response.getWriter().println("<h2>Registration Error</h2>");
            response.getWriter().println("<pre>");
            response.getWriter().println(e.getClass().getName());
            response.getWriter().println(": " + e.getMessage());
            response.getWriter().println("</pre>");

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

    // ==============================
    // WELCOME EMAIL
    // ==============================

    private void sendWelcomeEmail(
            String email,
            String name,
            String role) {

        try {

            if (APP_PASSWORD == null || APP_PASSWORD.trim().isEmpty()) {
                throw new IllegalStateException(
                        "SMART_ATTENDANCE_GMAIL_APP_PASSWORD is not set"
                );
            }

            Properties props = new Properties();

            props.put(
                    "mail.smtp.auth",
                    "true"
            );

            props.put(
                    "mail.smtp.starttls.enable",
                    "true"
            );

            props.put(
                    "mail.smtp.host",
                    "smtp.gmail.com"
            );

            props.put(
                    "mail.smtp.port",
                    "587"
            );

            Session mailSession =
                    Session.getInstance(
                            props,
                            new javax.mail.Authenticator() {

                                @Override
                                protected PasswordAuthentication
                                getPasswordAuthentication() {

                                    return new PasswordAuthentication(
                                            SENDER_EMAIL,
                                            APP_PASSWORD
                                    );
                                }
                            }
                    );

            Message message =
                    new MimeMessage(mailSession);

            message.setFrom(
                    new InternetAddress(
                            SENDER_EMAIL,
                            "Smart Attendance Team"
                    )
            );

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(email)
            );

            message.setSubject(
                    "Welcome to Smart Attendance"
            );

            String mailText =
                    "Hello " + name + ",\n\n"
                    + "Welcome to Smart Attendance!\n\n"
                    + "Your registration as "
                    + role
                    + " was successful.\n\n"
                    + "You can now login to the "
                    + "Smart Attendance System.\n\n"
                    + "Regards,\n"
                    + "Smart Attendance Team";

            message.setText(mailText);

            Transport.send(message);

            System.out.println(
                    "WELCOME EMAIL SENT TO: " + email
            );

        } catch (Exception e) {

            System.out.println("WELCOME EMAIL FAILED");
            System.out.println("Reason: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
