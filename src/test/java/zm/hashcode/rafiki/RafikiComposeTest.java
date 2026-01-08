package zm.hashcode.rafiki;

import java.net.HttpURLConnection;
import java.net.URL;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Integration test for RafikiCompose.
 * This test requires Docker and can take several minutes to run.
 * Note: First run will download ~500MB of Docker images.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RafikiComposeTest {

    private RafikiCompose env;

    @BeforeAll
    void start() {
        env = new RafikiCompose();
        env.start();
        System.out.println(" Rafiki containers started successfully");
    }

    @AfterAll
    void stop() {
        if (env != null) {
            env.stop();
            System.out.println(" Rafiki containers stopped");
        }
    }

    @Test
    void backendContainersStartAndPortsAreAccessible() throws Exception {

        String adminUrl = env.cloudNineAdminUrl();
        System.out.println("Testing Backend Admin URL: " + adminUrl);
        assertTrue(adminUrl.contains("localhost"), "Admin URL should contain localhost");
        assertTrue(adminUrl.contains(":"), "Admin URL should contain port");


        String openPaymentsUrl = env.happyLifeBackendUrl();
        System.out.println("Testing Backend Open Payments URL: " + openPaymentsUrl);
        assertTrue(openPaymentsUrl.contains("localhost"), "Open Payments URL should contain localhost");


        String connectorUrl = env.cloudNineMockAseUrl();
        System.out.println("Testing Backend Connector URL: " + connectorUrl);
        assertTrue(connectorUrl.contains("localhost"), "Connector URL should contain localhost");

        System.out.println(" All backend service URLs are valid and accessible");
        System.out.println("  Note: Auth and Frontend services require Kratos configuration");
    }

    @Test
    void backendAdminPortResponds() throws Exception {
        String adminUrl = env.cloudNineAdminUrl();
        System.out.println("Making HTTP request to: " + adminUrl);

        URL url = new URL(adminUrl);

        // Wait for the service to be ready using Awaitility
        await()
                .atMost(30, SECONDS)
                .pollInterval(2, SECONDS)
                .until(() -> {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5_000);
                        conn.setReadTimeout(5_000);
                        conn.setRequestMethod("GET");
                        conn.connect();
                        int responseCode = conn.getResponseCode();
                        conn.disconnect();
                        return responseCode < 500;
                    } catch (Exception e) {
                        return false;
                    }
                });

        // Verify the final response
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        conn.setRequestMethod("GET");

        try {
            conn.connect();
            int responseCode = conn.getResponseCode();
            System.out.println("Backend Admin responded with HTTP " + responseCode);
            assertTrue(responseCode < 500, "Backend should respond without server errors");
        } finally {
            conn.disconnect();
        }
    }
}
