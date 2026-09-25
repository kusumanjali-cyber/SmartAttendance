import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class AppSchedulerListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {

        System.out.println(
            "=========================================="
        );

        System.out.println(
            "SMARTATTEND ATTENDANCE REMINDER STARTED"
        );

        System.out.println(
            "=========================================="
        );

        scheduler =
            Executors.newSingleThreadScheduledExecutor();

        /*
         * Check timetable every 1 minute.
         *
         * If a period has ended and attendance
         * was not marked, the reminder service
         * will detect it.
         */

        scheduler.scheduleAtFixedRate(
            new Runnable() {

                @Override
                public void run() {

                    try {

                        AttendanceReminderService
                            .checkAttendanceReminders();

                    } catch (Exception e) {

                        System.out.println(
                            "Scheduler error: "
                            + e.getMessage()
                        );

                        e.printStackTrace();
                    }
                }
            },

            10,
            1,
            TimeUnit.MINUTES
        );
    }

    @Override
    public void contextDestroyed(
            ServletContextEvent event) {

        System.out.println(
            "SMARTATTEND ATTENDANCE REMINDER STOPPED"
        );

        if (scheduler != null &&
            !scheduler.isShutdown()) {

            scheduler.shutdownNow();
        }
    }
}