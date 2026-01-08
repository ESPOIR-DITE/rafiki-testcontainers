package zm.hashcode.rafiki;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.net.URL;

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
     * All services are configured to wait up to 3 minutes for startup.
     */
    @SuppressWarnings("resource")
    public RafikiCompose() {
        // Use classpath resource directly
        URL resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource("docker-compose.yml"),
                "docker-compose.yml not found in classpath");
        File composeFile;
        try {
            composeFile = Paths.get(resource.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load docker-compose.yml from classpath", e);
        }
        this.compose = new ComposeContainer(composeFile)
                .withLocalCompose(true)
                .withExposedService("cloud-nine-auth-1", 3003,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("cloud-nine-admin-1", 3010,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("cloud-nine-backend-1", 3000,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("cloud-nine-mock-ase-1", 3030,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("happy-life-backend-1", 4000,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("happy-life-auth-1", 4003,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("happy-life-admin-1", 4010,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService("happy-life-mock-ase-1", 3031,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));

    }

    /**
     * Starts all Rafiki containers and waits for them to be ready.
     * <p>
     * This method blocks until all backend services are listening on their ports
     * (up to 3 minutes per service). On first run, Docker images will be
     * downloaded.
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
     * This method cleans up all containers, networks, and volumes created by this
     * instance.
     */
    public void stop() {
        compose.stop();
    }

    /**
     * Returns the URL for the Cloud Nine Admin API endpoint.
     * <p>
     * This endpoint provides administrative operations for the Cloud Nine service.
     *
     * @return the Admin API URL (e.g., "http://localhost:4010")
     */
    public String cloudNineAdminUrl() {
        return getServiceUrl("cloud-nine-admin-1", 3010);
    }

    /**
     * Returns the URL for the Happy Life Backend API endpoint.
     * <p>
     * This endpoint handles backend operations for the Happy Life service.
     *
     * @return the Backend API URL (e.g., "http://localhost:4001")
     */
    public String happyLifeBackendUrl() {
        return getServiceUrl("happy-life-backend-1", 4001);
    }

    /**
     * Returns the URL for the Happy Life Auth API endpoint.
     * <p>
     * This endpoint handles authentication operations for the Happy Life service.
     *
     * @return the Auth API URL (e.g., "http://localhost:4003")
     */
    public String happyLifeAuthUrl() {
        return getServiceUrl("happy-life-auth-1", 4003);
    }

    /**
     * Returns the URL for the Cloud Nine Backend API endpoint.
     * <p>
     * This endpoint handles backend operations for the Cloud Nine service.
     *
     * @return the Backend API URL (e.g., "http://localhost:4000")
     */
    public String cloudNineBackendUrl() {
        return getServiceUrl("cloud-nine-backend-1", 4000);
    }

    /**
     * Returns the URL for the Cloud Nine Mock ASE endpoint.
     * <p>
     * This endpoint provides a mock ASE (Application Service Environment) for
     * testing.
     *
     * @return the Mock ASE URL (e.g., "http://localhost:3030")
     */
    public String cloudNineMockAseUrl() {
        return getServiceUrl("cloud-nine-mock-ase-1", 3030);
    }

    /**
     * Returns the URL for the Cloud Nine Auth API endpoint.
     * <p>
     * This endpoint handles authentication operations for the Cloud Nine service.
     *
     * @return the Auth API URL (e.g., "http://localhost:3002")
     */
    public String cloudNineAuthUrl() {
        return getServiceUrl("cloud-nine-auth-1", 3002);
    }

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
