package zm.hashcode.rafiki;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Testcontainers wrapper for Rafiki - the open-source Interledger service.
 * <p>
 * This class manages a Docker Compose stack containing:
 * <ul>
 * <li>Rafiki Backend (Open Payments, Admin API, ILP Connector)</li>
 * <li>PostgreSQL database</li>
 * <li>Redis cache</li>
 * <li>TigerBeetle accounting database</li>
 * </ul>
 * <p>
 * Note: Authentication and Frontend services are not included in this
 * testcontainer
 * configuration for simplicity. They require additional Kratos identity server
 * setup.
 *
 * <h2>Example Usage:</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;TestInstance(TestInstance.Lifecycle.PER_CLASS)
 *     public class MyIntegrationTest {
 *         private RafikiCompose rafiki;
 *
 *         &#64;BeforeAll
 *         void setup() {
 *             rafiki = new RafikiCompose();
 *             rafiki.start();
 *         }
 *
 *         &#64;AfterAll
 *         void teardown() {
 *             rafiki.stop();
 *         }
 *
 *         @Test
 *         void testBackend() {
 *             String adminUrl = rafiki.backendAdminUrl();
 *             // Make HTTP requests to test Rafiki
 *         }
 *     }
 * }
 * </pre>
 *
 * @see <a href="https://rafiki.dev">Rafiki Documentation</a>
 * @see <a href="https://github.com/hashcode-zm/rafiki-testcontainers">GitHub
 *      Repository</a>
 * @since 0.1.0
 */
public class RafikiCompose implements AutoCloseable {

    private final ComposeContainer compose;

    /**
     * Creates a new Rafiki testcontainer instance.
     * <p>
     * The containers are not started until {@link #start()} is called.
     * All services are configured to wait up to 2 minutes for startup.
     */
    @SuppressWarnings("resource")
    public RafikiCompose() {
        // Use classpath resource directly
        URL resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource("rafiki-local-containers/docker-compose.yml"),
                "docker-compose.yml not found in classpath");
        File composeFile;
        try {
            composeFile = Paths.get(resource.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load docker-compose.yml from classpath", e);
        }
        this.compose = new ComposeContainer(composeFile)
                .withExposedService("cloud-nine-auth-1", 3003,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("cloud-nine-admin-1", 3010,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("cloud-nine-backend-1", 3000,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("cloud-nine-mock-ase-1", 3030,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("happy-life-backend-1", 4000,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("happy-life-auth-1", 4003,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("happy-life-admin-1", 4010,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
                .withExposedService("happy-life-mock-ase-1", 3031,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    }

    /**
     * Starts all Rafiki containers and waits for them to be ready.
     * <p>
     * This method initializes and starts the Docker Compose stack containing:
     * <ul>
     * <li>Cloud Nine Wallet services (backend, auth, admin, mock ASE)</li>
     * <li>Happy Life Bank services (backend, auth, admin, mock ASE)</li>
     * <li>Shared infrastructure (PostgreSQL database, Redis cache)</li>
     * </ul>
     * <p>
     * The method blocks until all exposed services are listening on their ports
     * (up to 2 minutes per service). On first run, Docker images will be
     * downloaded, which may take several minutes.
     *
     * @throws org.testcontainers.containers.ContainerLaunchException if containers
     *                                                                fail to start
     */
    public void start() {
        compose.start();
    }

    /**
     * Stops and removes all Rafiki containers.
     * <p>
     * This method gracefully stops all containers in the Docker Compose stack
     * and cleans up associated resources. Containers, networks, and volumes
     * created by this instance are removed.
     * <p>
     * Note: Data volumes are preserved by default. To completely remove volumes,
     * use Docker Compose commands directly.
     */
    public void stop() {
        compose.stop();
    }

    // ============================================
    // Cloud Nine Wallet Service Getters
    // ============================================

    /**
     * Returns the URL for the Cloud Nine Backend API endpoint.
     * <p>
     * This endpoint provides the Open Payments API (port 80) for the Cloud Nine Wallet.
     * The backend also exposes Admin API on port 3001 and ILP Connector on port 3002.
     *
     * @return the Backend Open Payments API URL (e.g., "http://localhost:3000")
     */
    public String cloudNineBackendUrl() {
        return getServiceUrl("cloud-nine-backend-1", 3000);
    }

    /**
     * Returns the URL for the Cloud Nine Auth API endpoint.
     * <p>
     * This endpoint provides GNAP (Grant Negotiation and Authorization Protocol) 
     * authorization services for the Cloud Nine Wallet. The auth service also exposes
     * ports 3006 (grant endpoint), 3009, and 3011 (service API).
     *
     * @return the Auth API URL (e.g., "http://localhost:3003")
     */
    public String cloudNineAuthUrl() {
        return getServiceUrl("cloud-nine-auth-1", 3003);
    }

    /**
     * Returns the URL for the Cloud Nine Admin UI endpoint.
     * <p>
     * This endpoint provides a web-based administrative interface for managing
     * the Cloud Nine Wallet Rafiki instance.
     *
     * @return the Admin UI URL (e.g., "http://localhost:3010")
     */
    public String cloudNineAdminUrl() {
        return getServiceUrl("cloud-nine-admin-1", 3010);
    }

    /**
     * Returns the URL for the Cloud Nine Mock ASE endpoint.
     * <p>
     * This endpoint provides a mock Account Servicing Entity (ASE) interface
     * for testing user-facing wallet operations with the Cloud Nine Wallet.
     *
     * @return the Mock ASE URL (e.g., "http://localhost:3030")
     */
    public String cloudNineMockAseUrl() {
        return getServiceUrl("cloud-nine-mock-ase-1", 3030);
    }

    // ============================================
    // Happy Life Bank Service Getters
    // ============================================

    /**
     * Returns the URL for the Happy Life Backend API endpoint.
     * <p>
     * This endpoint provides the Open Payments API (port 80) for the Happy Life Bank.
     * The backend also exposes Admin API on port 3001 and ILP Connector on port 3002.
     *
     * @return the Backend Open Payments API URL (e.g., "http://localhost:4000")
     */
    public String happyLifeBackendUrl() {
        return getServiceUrl("happy-life-backend-1", 4000);
    }

    /**
     * Returns the URL for the Happy Life Auth API endpoint.
     * <p>
     * This endpoint provides GNAP (Grant Negotiation and Authorization Protocol)
     * authorization services for the Happy Life Bank. The auth service also exposes
     * ports 4006 (grant endpoint), 4009, and 4011 (service API).
     *
     * @return the Auth API URL (e.g., "http://localhost:4003")
     */
    public String happyLifeAuthUrl() {
        return getServiceUrl("happy-life-auth-1", 4003);
    }

    /**
     * Returns the URL for the Happy Life Admin UI endpoint.
     * <p>
     * This endpoint provides a web-based administrative interface for managing
     * the Happy Life Bank Rafiki instance.
     *
     * @return the Admin UI URL (e.g., "http://localhost:4010")
     */
    public String happyLifeAdminUrl() {
        return getServiceUrl("happy-life-admin-1", 4010);
    }

    /**
     * Returns the URL for the Happy Life Mock ASE endpoint.
     * <p>
     * This endpoint provides a mock Account Servicing Entity (ASE) interface
     * for testing user-facing bank operations with the Happy Life Bank.
     *
     * @return the Mock ASE URL (e.g., "http://localhost:3031")
     */
    public String happyLifeMockAseUrl() {
        return getServiceUrl("happy-life-mock-ase-1", 3031);
    }

    /**
     * Helper method to get the URL for a service by name and internal port.
     * <p>
     * Retrieves the mapped host port for a service and constructs a URL.
     * If the service is not started or the port cannot be determined,
     * returns a placeholder URL indicating the service is not available.
     *
     * @param serviceName the Docker Compose service name
     * @param port the internal container port
     * @return the service URL with mapped host port, or a placeholder if unavailable
     */
    private String getServiceUrl(String serviceName, int port) {
        try {
            Integer mappedPort = compose.getServicePort(serviceName, port);
            if (mappedPort == null) {
                return "http://localhost:" + port + " (not started)";
            }
            return "http://localhost:" + mappedPort;
        } catch (IllegalStateException | NullPointerException e) {
            return "http://localhost:" + port + " (not started)";
        }
    }

    @Override
    public void close() {
        stop();
    }
}
