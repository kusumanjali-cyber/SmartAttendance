import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EmailJSService {

    private static final String API_URL =
            "https://api.emailjs.com/api/v1.0/email/send";

    private static final String SERVICE_ID =
            "service_utrl2ct";

    private static final String PUBLIC_KEY =
            "3om3V0_tb1ckVZWlS";

    private static final String REGISTRATION_TEMPLATE =
            "template_g3ebs0w";

    // Private Key is read from environment variable.
    // Never hardcode it or print it.
    private static final String PRIVATE_KEY =
            System.getenv("EMAILJS_PRIVATE_KEY");


    // =========================================================
    // REGISTRATION / WELCOME EMAIL
    // =========================================================

    public static boolean sendRegistrationEmail(
            String email,
            String name,
            String role) {

        if (email == null || email.trim().isEmpty()) {
            System.out.println("ERROR: Email is empty.");
            return false;
        }

        if (PRIVATE_KEY == null || PRIVATE_KEY.trim().isEmpty()) {
            System.out.println(
                    "ERROR: EMAILJS_PRIVATE_KEY environment variable is missing."
            );
            return false;
        }

        String params =
                "{"
                + "\"name\":\"" + escape(name) + "\","
                + "\"email\":\"" + escape(email) + "\","
                + "\"role\":\"" + escape(role) + "\","
                + "\"to_email\":\"" + escape(email) + "\""
                + "}";

        return send(REGISTRATION_TEMPLATE, params);
    }


    // =========================================================
    // SEND EMAIL THROUGH EMAILJS
    // =========================================================

    private static boolean send(
            String templateId,
            String params) {

        HttpURLConnection connection = null;

        try {

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

            connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/153.0.0.0 Safari/537.36"
);

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);


            // IMPORTANT:
            // accessToken = EmailJS Private Key
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

                    + "\"accessToken\":\""
                    + escape(PRIVATE_KEY)
                    + "\","

                    + "\"template_params\":"
                    + params

                    + "}";


            System.out.println(
                    "========== EMAILJS REQUEST =========="
            );

            System.out.println(
                    "Service ID     : " + SERVICE_ID
            );

            System.out.println(
                    "Template ID    : " + templateId
            );

            System.out.println(
                    "Public Key     : configured"
            );

            System.out.println(
                    "Private Key    : configured"
            );

            System.out.println(
                    "====================================="
            );


            try (OutputStream output =
                         connection.getOutputStream()) {

                output.write(
                        json.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                output.flush();
            }


            int responseCode =
                    connection.getResponseCode();

            InputStream stream;

            if (responseCode >= 200 &&
                responseCode < 300) {

                stream =
                        connection.getInputStream();

            } else {

                stream =
                        connection.getErrorStream();
            }


            String responseMessage = "";

            if (stream != null) {

                responseMessage =
                        readStream(stream);

                stream.close();
            }


            System.out.println(
                    "========== EMAILJS RESPONSE =========="
            );

            System.out.println(
                    "HTTP Code: " + responseCode
            );

            System.out.println(
                    "Response: " + responseMessage
            );

            System.out.println(
                    "======================================"
            );


            return responseCode >= 200 &&
                   responseCode < 300;

        } catch (Exception e) {

            System.out.println(
                    "EMAILJS ERROR: "
                    + e.getMessage()
            );

            e.printStackTrace();

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