import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public class AttendanceReminderService {

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/smartattendance"
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";

    public static void checkAttendanceReminders() {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        String dayName = today.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        String sql =
            "SELECT t.id, t.period_number, t.start_time, t.end_time, " +
            "s.subject_name, f.faculty_name, f.email, t.subject_id " +
            "FROM timetable t " +
            "JOIN subjects s ON t.subject_id = s.id " +
            "LEFT JOIN faculty f ON s.faculty_id = f.id " +
            "WHERE LOWER(t.day_name) = LOWER(?) " +
            "AND t.active = TRUE " +
            "AND t.end_time < ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection con =
                    DriverManager.getConnection(
                        DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps =
                    con.prepareStatement(sql)) {

                ps.setString(1, dayName);
                ps.setTime(2, java.sql.Time.valueOf(now));

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        int timetableId =
                            rs.getInt("id");

                        int periodNumber =
                            rs.getInt("period_number");

                        int subjectId =
                            rs.getInt("subject_id");

                        String subjectName =
                            rs.getString("subject_name");

                        String facultyName =
                            rs.getString("faculty_name");

                        String facultyEmail =
                            rs.getString("email");

                        LocalTime endTime =
                            rs.getTime("end_time")
                               .toLocalTime();

                        boolean attendanceMarked =
                            isAttendanceMarked(
                                con,
                                subjectId,
                                today
                            );

                        if (!attendanceMarked) {

                            System.out.println(
                                "ATTENDANCE REMINDER: " +
                                subjectName +
                                " | Period " +
                                periodNumber +
                                " | Faculty: " +
                                facultyName
                            );

                            System.out.println(
                                "Attendance was not marked " +
                                "before period ended at " +
                                endTime
                            );

                            // Prevent the same reminder from
                            // being repeatedly processed.
                            saveReminder(
                                con,
                                timetableId,
                                today
                            );

                            // Email will be connected
                            // in the next step.
                            if (facultyEmail != null &&
                                !facultyEmail.trim().isEmpty()) {

                                System.out.println(
                                    "Reminder should be sent to: "
                                    + facultyEmail
                                );
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {

            System.out.println(
                "Attendance reminder error: "
                + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static boolean isAttendanceMarked(
            Connection con,
            int subjectId,
            LocalDate date) throws Exception {

        String sql =
            "SELECT COUNT(*) AS total " +
            "FROM attendance_records " +
            "WHERE subject_id = ? " +
            "AND attendance_date = ?";

        try (PreparedStatement ps =
                con.prepareStatement(sql)) {

            ps.setInt(1, subjectId);
            ps.setDate(
                2,
                java.sql.Date.valueOf(date)
            );

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        }

        return false;
    }

    private static void saveReminder(
            Connection con,
            int timetableId,
            LocalDate date) throws Exception {

        String checkSql =
            "SELECT id FROM attendance_reminders " +
            "WHERE timetable_id = ? " +
            "AND reminder_date = ?";

        try (PreparedStatement ps =
                con.prepareStatement(checkSql)) {

            ps.setInt(1, timetableId);
            ps.setDate(
                2,
                java.sql.Date.valueOf(date)
            );

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return;
                }
            }
        }

        String insertSql =
            "INSERT INTO attendance_reminders " +
            "(timetable_id, reminder_date, reminder_time) " +
            "VALUES (?, ?, NOW())";

        try (PreparedStatement ps =
                con.prepareStatement(insertSql)) {

            ps.setInt(1, timetableId);
            ps.setDate(
                2,
                java.sql.Date.valueOf(date)
            );

            ps.executeUpdate();
        }
    }
}