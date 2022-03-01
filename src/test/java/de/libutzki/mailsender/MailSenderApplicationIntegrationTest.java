package de.libutzki.mailsender;

import static org.testcontainers.Testcontainers.exposeHostPorts;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import com.microsoft.playwright.Playwright;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.libutzki.mailsender.KeycloakClient.RealmClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class MailSenderApplicationIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.1").withUsername("postgres")
			.withPassword("test");

	@Container
	static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:17.0.0")
			.withAccessToHost(true);

	@Container
	static GenericContainer<?> chrome = new GenericContainer<>(DockerImageName.parse("browserless/chrome:latest"))
			.withExtraHost("host.docker.internal","host-gateway")
			.withAccessToHost(true)
			.withExposedPorts(3000)
			.waitingFor(Wait.forHttp("/"));

	@LocalServerPort
	private Integer port;

	private static final Path screenshotPath = Paths.get("target", "playwright");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@DynamicPropertySource
	static void configureKeycloak(DynamicPropertyRegistry registry) {
		registry.add("keycloak.auth-server-url", MailSenderApplicationIntegrationTest::getAuthServerURL);
	}

	private static String getAuthServerURL() {
		return String.format("http://host.docker.internal:%s%s", keycloakContainer.getHttpPort(),
				keycloakContainer.getContextPath());
	}

	@BeforeEach
	void init(@Autowired KeycloakSpringBootProperties keycloakProperties) {
		exposeHostPorts(port);
		try (KeycloakClient keycloakClient = new KeycloakClient(
				KeycloakBuilder.builder()
						.serverUrl(keycloakContainer.getAuthServerUrl())
						.grantType(OAuth2Constants.PASSWORD)
						.realm(KeycloakContainer.MASTER_REALM)
						.clientId(KeycloakContainer.ADMIN_CLI_CLIENT)
						.username(keycloakContainer.getAdminUsername())
						.password(keycloakContainer.getAdminPassword())
						.resteasyClient(
								new ResteasyClientBuilder()
										.connectionPoolSize(10)
										.build())
						.build())) {
			final RealmClient realm = keycloakClient.createRealm(keycloakProperties.getRealm());
			realm.createClient(keycloakProperties.getResource(),
					String.format("http://host.docker.internal:%s/*", port));
			realm.createUser("user1", "bmbm");

		}
	}

	@Test
	void testApplication() {

		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium()
					.connectOverCDP("ws://" + chrome.getHost() + ":" + chrome.getFirstMappedPort() + "");
			String baseUrl = String.format("http://host.docker.internal:%d", port);
			try (BrowserContext browserContext = browser.newContext(new Browser.NewContextOptions().setRecordVideoDir(Paths.get("target/videos")));
					Page page = browserContext.newPage()) {
				page.navigate(baseUrl + "/");
				page.waitForLoadState();
				page.screenshot(new ScreenshotOptions().setPath(screenshotPath.resolve("login-screen.png")));
				page.locator("id=username").fill("user1");
				page.locator("id=password").fill("bmbm");
				page.locator("id=kc-login").click();
				page.waitForLoadState();
				page.screenshot(new ScreenshotOptions().setPath(screenshotPath.resolve("after-login.png")));
				page.locator("id=logout_button").click();
				page.waitForLoadState();
				page.screenshot(new ScreenshotOptions().setPath(screenshotPath.resolve("after-logout.png")));
			}
		}
	}

}
