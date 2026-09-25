import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailJSService {

    private static final String API_URL =
            "https://api.emailjs.com/api/v1.0/email/send";

    // =========================================================
    // EMAILJS CONFIGURATION
    // =========================================================

    private static final String SERVICE_ID =
            "service_fu26rgv";

    private static final String PUBLIC_KEY =
            "3om3V0_tb1ckVZWlS";

    private static final String REGISTRATION_TEMPLATE =
            "template_l36pz5n";

    private static final String MARKS_TEMPLATE =
            "template_j2ajnx4";


    // =========================================================
    // REGISTRATION EMAIL
    // =========================================================

    public static boolean sendRegistrationEmail(
            String email,
            String name,
            String role) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("SMARTATTEND - REGISTRATION EMAIL");
        System.out.println("==========================================");

        if (email == null || email.trim().isEmpty()) {
            System.out.println("ERROR: Student/User email is empty.");
            return false;
        }

        String params =
                "{"
                + "\"name\":\"" + escape(name) + "\","
                + "\"email\":\"" + escape(email) + "\","
                + "\"role\":\"" + escape(role) + "\","
                + "\"to_email\":\"" + escape(email) + "\""
                + "}";

        return send(
                REGISTRATION_TEMPLATE,
                params
        );
    }


    // =========================================================
    // MARKS NOTIFICATION
    // =========================================================

    public static boolean sendMarksNotification(
            String email,
            String name,
            String subject,
            String marks) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("SMARTATTEND - MARKS EMAIL");
        System.out.println("==========================================");

        if (email == null || email.trim().isEmpty()) {
            System.out.println("ERROR: Student email is empty.");
            return false;
        }

        String params =
                "{"
                + "\"name\":\"" + escape(name) + "\","
                + "\"email\":\"" + escape(email) + "\","
                + "\"to_email\":\"" + escape(email) + "\","
                + "\"subject\":\"" + escape(subject) + "\","
                + "\"marks\":\"" + escape(marks) + "\""
                + "}";

        return send(
                MARKS_TEMPLATE,
                params
        );
    }


    // =========================================================
    // COMMON EMAILJS SEND METHOD
    // =========================================================

    private static boolean send(
            String templateId,
            String params) {

        HttpURLConnection connection = null;

        try {

            System.out.println("EmailJS URL: " + API_URL);
            System.out.println("Service ID: " + SERVICE_ID);
            System.out.println("Template ID: " + templateId);
            System.out.println("Template Params: " + params);

            URL url = new URL(API_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            connection.setDoOutput(true);


            // =================================================
            // EMAILJS JSON REQUEST
            // =================================================

            String json =
                    "{"
                    + "\"service_id\":\""
                    + escape(SERVICE_ID)
                    + "\","

                    + "\"template_id\":\""
                    + escape(templateId)
                    + "\","

                    + "\"user_id\":\""
                    + escape(PUBLIC_KEY)
                    + "\","

                    + "\"template_params\":"
                    + params

                    + "}";


            System.out.println();
            System.out.println("========== EMAILJS REQUEST ==========");
            System.out.println(json);
            System.out.println("=====================================");


            // =================================================
            // SEND
            // =================================================

            try (OutputStream output =
                         connection.getOutputStream()) {

                byte[] data =
                        json.getBytes(StandardCharsets.UTF_8);

                output.write(data);
                output.flush();
            }


            // =================================================
            // RESPONSE
            // =================================================

            int responseCode =
                    connection.getResponseCode();

            InputStream stream;

            if (responseCode >= 200 &&
                responseCode < 300) {

                stream = connection.getInputStream();

            } else {

                stream = connection.getErrorStream();
            }


            String responseMessage = "";

            if (stream != null) {

                responseMessage =
                        readStream(stream);

                stream.close();
            }


            System.out.println();
            System.out.println("========== EMAILJS RESPONSE ==========");
            System.out.println(
                    "HTTP Code: " + responseCode
            );

            System.out.println(
                    "Response: " + responseMessage
            );

            System.out.println("======================================");


            if (responseCode >= 200 &&
                responseCode < 300) {

                System.out.println(
                        "SUCCESS: EmailJS accepted the email."
                );

                return true;
            }


            System.out.println(
                    "FAILED: EmailJS rejected the request."
            );

            return false;


        } catch (Exception e) {

            System.out.println();
            System.out.println("========== EMAILJS JAVA ERROR ==========");

            System.out.println(
                    "Error: " + e.getMessage()
            );

            e.printStackTrace();

            System.out.println(
                    "=========================================");

            return false;


        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    // =========================================================
    // READ RESPONSE
    // =========================================================

    private static String readStream(
            InputStream stream)
            throws Exception {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[1024];

        int length;

        while ((length =
                stream.read(buffer)) != -1) {

            output.write(
                    buffer,
                    0,
                    length
            );
        }

        return output.toString(
                StandardCharsets.UTF_8.name()
        );
    }


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private static String escape(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

