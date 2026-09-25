
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public static boolean sendEmail(
            String recipient,
            String subject,
            String body) {

        String sender = System.getenv("SMARTATTEND_EMAIL");
        String appPassword =
                System.getenv("SMARTATTEND_APP_PASSWORD");

        if (sender == null || sender.trim().isEmpty()) {
            System.out.println(
                    "ERROR: SMARTATTEND_EMAIL is not configured."
            );
            return false;
        }

        if (appPassword == null || appPassword.trim().isEmpty()) {
            System.out.println(
                    "ERROR: SMARTATTEND_APP_PASSWORD is not configured."
            );
            return false;
        }

        if (recipient == null || recipient.trim().isEmpty()) {
            System.out.println(
                    "ERROR: Recipient email is empty."
            );
            return false;
        }

        try {
            Properties properties = new Properties();

            properties.put("mail.smtp.host", SMTP_HOST);
            properties.put("mail.smtp.port", SMTP_PORT);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(
                    properties,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication
                        getPasswordAuthentication() {

                            return new PasswordAuthentication(
                                    sender,
                                    appPassword
                            );
                        }
                    }
            );

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(sender));

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipient)
            );

            message.setSubject(subject);
message.setText(body);

            Transport.send(message);

            System.out.println(
                    "EMAIL SENT SUCCESSFULLY TO: " + recipient
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "EMAIL SENDING FAILED: " + e.getMessage()
            );

            e.printStackTrace();

            return false;
        }
    }

    // Registration email - 2 parameters
    public static boolean sendRegistrationEmail(
            String receiverEmail,
            String role) {

        return sendRegistrationEmail(
                receiverEmail,
                "User",
                role
        );
    }

    // Registration email - 3 parameters
    public static boolean sendRegistrationEmail(
            String receiverEmail,
            String name,
            String role) {

        String subject =
                "Welcome to Smart Attendance - Registration Successful";

        String body =
                "Hello " + name + ",\n\n"
                + "Welcome to Smart Attendance!\n\n"
                + "Your registration was successful.\n\n"
                + "Assigned Role: " + role + "\n"
                + "Account Status: Active\n\n"
                + "You can now login using your registered email.\n\n"
                + "Regards,\n"
                + "Smart Attendance Team";

        return sendEmail(
                receiverEmail,
                subject,
                body
        );
    }

    // Student marks notification
    public static boolean sendMarksNotification(
            String studentEmail,
            String studentName,
            String subjectName,
            String examType,
            double marks,
            double maximumMarks) {

        String subject =
                "SmartAttend - Marks Updated";

        String body =
                "Hello " + studentName + ",\n\n"
                + "Your marks have been updated in SmartAttend.\n\n"
                + "Subject: " + subjectName + "\n"
                + "Examination: " + examType + "\n"
                + "Marks: " + marks + " / "
                + maximumMarks + "\n\n"
                + "Please login to SmartAttend to view your marks.\n\n"
                + "Regards,\n"
                + "SmartAttend";

        return sendEmail(
                studentEmail,
                subject,
                body
        );
    }

    // Faculty attendance reminder
    public static boolean sendAttendanceReminder(
            String facultyEmail,
            String facultyName,
            String subjectName,
            int periodNumber) {

        String subject =
                "SmartAttend - Attendance Reminder";

        String body =
                "Hello " + facultyName + ",\n\n"
                + "Attendance reminder from SmartAttend.\n\n"
                + "Subject: " + subjectName + "\n"
                + "Period: " + periodNumber + "\n\n"
                + "Please mark attendance as soon as possible.\n\n"
                + "Regards,\n"
                + "SmartAttend";

        return sendEmail(
                facultyEmail,
                subject,
                body
        );
    }
}