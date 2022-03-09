package de.libutzki.mailsender.integration;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import com.microsoft.playwright.Playwright;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.libutzki.mailsender.integration.KeycloakClient.RealmClient;
import io.restassured.RestAssured;

@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT )
@DirtiesContext
@Testcontainers
@TestPropertySource( properties = {
		"spring.datasource.url=jdbc:tc:postgresql:14.1:///testdb",
} )
@DisabledIf( "java.awt.GraphicsEnvironment#isHeadless" )
class Stage_07_MailSenderApplicationLocalBrowserIntegrationTest {

	private static final String hostname = "host.docker.internal";

	private static final Path screenshotAndVideoPath = Paths.get( "target", "playwright" );

	private static final User user1 = new User( "user1", "password1", "user1@example.com" );
	private static final User user2 = new User( "user2", "password2", "user2@example.com" );
	@Container
	static KeycloakContainer keycloakContainer = new KeycloakContainer( "quay.io/keycloak/keycloak:17.0.0" );

	private static final Integer MAILHOG_SMTP_PORT = 1025;
	private static final Integer MAILHOG_HTTP_PORT = 8025;

	@Container
	static GenericContainer<?> mailhogContainer = new GenericContainer<>( "mailhog/mailhog:v1.0.1" )
			.withExposedPorts( MAILHOG_SMTP_PORT, MAILHOG_HTTP_PORT )
			.waitingFor(
					Wait
							.forHttp( "/" )
							.forPort( MAILHOG_HTTP_PORT ) );

	@DynamicPropertySource
	static void configureKeycloak( final DynamicPropertyRegistry registry ) {
		registry.add( "keycloak.auth-server-url", Stage_07_MailSenderApplicationLocalBrowserIntegrationTest::getAuthServerURL );
	}

	private static String getAuthServerURL( ) {
		return String.format(
				"http://%s:%s%s",
				hostname,
				keycloakContainer.getHttpPort( ),
				keycloakContainer.getContextPath( ) );
	}

	@DynamicPropertySource
	static void configureMail( final DynamicPropertyRegistry registry ) {
		registry.add( "spring.mail.host", mailhogContainer::getHost );
		registry.add( "spring.mail.port", ( ) -> mailhogContainer.getMappedPort( MAILHOG_SMTP_PORT ) );
	}

	@LocalServerPort
	private Integer port;

	@BeforeEach
	void init( @Autowired final KeycloakSpringBootProperties keycloakProperties ) {
		try ( KeycloakClient keycloakClient = new KeycloakClient(
				KeycloakBuilder
						.builder( )
						.serverUrl( keycloakContainer.getAuthServerUrl( ) )
						.grantType( OAuth2Constants.PASSWORD )
						.realm( KeycloakContainer.MASTER_REALM )
						.clientId( KeycloakContainer.ADMIN_CLI_CLIENT )
						.username( keycloakContainer.getAdminUsername( ) )
						.password( keycloakContainer.getAdminPassword( ) )
						.resteasyClient(
								new ResteasyClientBuilder( )
										.connectionPoolSize( 10 )
										.build( ) )
						.build( ) ) ) {
			final RealmClient realm = keycloakClient.createRealm( keycloakProperties.getRealm( ) );
			realm.createClient( keycloakProperties.getResource( ), String.format( "http://%s:%s/*", hostname, port ) );
			realm.createUser( user1 );
			realm.createUser( user2 );

		}
	}

	@BeforeEach
	void setupRestAssured( ) {
		RestAssured.baseURI = String.format( "http://%s", mailhogContainer.getHost( ) );
		RestAssured.port = mailhogContainer.getMappedPort( MAILHOG_HTTP_PORT );
		RestAssured.basePath = "/api/v2";
	}

	@Test
	void testApplication( ) {

		try ( Playwright playwright = Playwright.create( ) ) {
			final Browser browser = playwright.chromium( )
					.launch( new BrowserType.LaunchOptions( ).setHeadless( false ).setSlowMo( 500 ) );
			final String baseUrl = String.format( "http://%s:%s", hostname, port );
			try (
					BrowserContext browserContext = browser.newContext( new Browser.NewContextOptions( ).setRecordVideoDir( screenshotAndVideoPath ) );
					Page page = browserContext.newPage( ) ) {
				page.navigate( baseUrl + "/" );
				page.waitForLoadState( );
				page.screenshot( new ScreenshotOptions( ).setPath( screenshotAndVideoPath.resolve( "login-screen.png" ) ) );

				// Login user 1
				final LoginPage loginPage = new LoginPage( page );
				loginPage.login( user1.username( ), user1.password( ) );
				page.waitForLoadState( );
				page.screenshot( new ScreenshotOptions( ).setPath( screenshotAndVideoPath.resolve( "after-login.png" ) ) );

				// Assert that no mails been added to sent mails table
				final Locator tableRowLocator = page.locator( "#sent-mails-table tbody tr" );
				assertThat( tableRowLocator ).hasCount( 0 );

				given( )
						.when( ).get( "/messages" )
						.then( ).body( "total", equalTo( 0 ) );

				// Send mail
				final Mail mail = new Mail( "test@example.com", "Test-Mail", "This is a test mail" );
				page.locator( "id=recipient" ).fill( mail.recipient( ) );
				page.locator( "id=subject" ).fill( mail.subject( ) );
				page.locator( "id=body" ).fill( mail.body( ) );
				page.locator( "id=send-mail-button" ).click( );

				// Assert sent mails table content

				assertThat( tableRowLocator ).hasCount( 1 );

				final Locator firstSentMail = tableRowLocator.nth( 0 );
				final Locator rowsOfFirstSentMail = firstSentMail.locator( "td" );
				assertThat( rowsOfFirstSentMail.nth( 0 ) ).hasText( mail.recipient( ) );
				assertThat( rowsOfFirstSentMail.nth( 1 ) ).hasText( mail.subject( ) );
				assertThat( rowsOfFirstSentMail.nth( 2 ) ).hasText( mail.body( ) );

				// Assert sent mails from Mailhog

				given( )
						.when( ).get( "/messages" )
						.then( )
						.body( "total", equalTo( 1 ) )
						.and( )
						.body( "items[0].Content.Headers.From", contains( user1.eMail( ) ) )
						.and( )
						.body( "items[0].Content.Headers.To", contains( mail.recipient( ) ) )
						.and( )
						.body( "items[0].Content.Headers.Subject", contains( mail.subject( ) ) )
						.and( )
						.body( "items[0].Content.Body", equalTo( mail.body( ) ) );

				// Logout
				page.locator( "id=logout_button" ).click( );
				page.waitForLoadState( );
				page.screenshot( new ScreenshotOptions( ).setPath( screenshotAndVideoPath.resolve( "after-logout.png" ) ) );

				// Login user 2
				loginPage.login( user2.username( ), user2.password( ) );

				// Assert that no mails been added to sent mails table
				assertThat( tableRowLocator ).hasCount( 0 );

				given( )
						.when( ).get( "/search?kind={kind}&query={sender}", "from", user2.eMail( ) )
						.then( ).body( "total", equalTo( 0 ) );

			}
		}
	}

}
