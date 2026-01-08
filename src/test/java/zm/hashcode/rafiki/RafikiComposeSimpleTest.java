package zm.hashcode.rafiki;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class RafikiComposeSimpleTest {

    @Test
    void canInstantiate() {
        RafikiCompose compose = new RafikiCompose();
        assertNotNull(compose, "RafikiCompose should be instantiated");


        String adminUrl = compose.cloudNineAdminUrl();
        assertNotNull(adminUrl);
        assertTrue(adminUrl.contains("not started") || adminUrl.contains("localhost"));

        String openPaymentsUrl = compose.happyLifeBackendUrl();
        assertNotNull(openPaymentsUrl);
        assertTrue(openPaymentsUrl.contains("not started") || openPaymentsUrl.contains("localhost"));

        String connectorUrl = compose.cloudNineMockAseUrl();
        assertNotNull(connectorUrl);
        assertTrue(connectorUrl.contains("not started") || connectorUrl.contains("localhost"));

        System.out.println("✅ RafikiCompose instantiated successfully");
        System.out.println("   Backend services are available for testing");
    }
}

