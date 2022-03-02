package de.libutzki.mailsender.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.WithMockKeycloakAuth;

import io.restassured.RestAssured;

@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT )
@DirtiesContext
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource( properties = {
		"spring.datasource.url=jdbc:tc:postgresql:14.1:///testdb",
		"keycloak.auth-server-url=http://dummy:9999/auth"
} )
class Stage_06_MailControllerWithPersistenceTest {

	@Autowired
	private MockMvc mockMvc;

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
	static void configureMail( final DynamicPropertyRegistry registry ) {
		registry.add( "spring.mail.host", mailhogContainer::getHost );
		registry.add( "spring.mail.properties.mail.smtp.port", ( ) -> mailhogContainer.getMappedPort( MAILHOG_SMTP_PORT ) );
	}

	@BeforeEach
	void setupRestAssured( ) {
		RestAssured.baseURI = String.format( "http://%s", mailhogContainer.getHost( ) );
		RestAssured.port = mailhogContainer.getMappedPort( MAILHOG_HTTP_PORT );
		RestAssured.basePath = "/api/v2";
	}

	@Test
	@WithMockKeycloakAuth( claims = @OpenIdClaims( email = "sender@example.com" ) )
	public void testWithAuth( ) throws Exception {

		mockMvc.perform( get( "/" ) )
				.andExpect( status( ).isOk( ) );
	}

	@Test
	public void testWithoutAuth( ) throws Exception {

		mockMvc.perform( get( "/" ) )
				.andExpect( status( ).is3xxRedirection( ) );
	}

	@Test
	@WithMockKeycloakAuth( claims = @OpenIdClaims( email = "sender@example.com" ) )
	public void testSendMail( ) throws Exception {

		given( )
				.when( ).get( "/messages" )
				.then( ).body( "total", equalTo( 0 ) );

		mockMvc.perform( post( "/" )
				.contentType( MediaType.APPLICATION_FORM_URLENCODED )
				.with( csrf( ) )
				.param( "recipient", "recipient@example.com" )
				.param( "subject", "Test-Subject" )
				.param( "body", "Test-Body" ) )

				.andExpect( status( ).is3xxRedirection( ) );

		given( )
				.when( ).get( "/messages" )
				.then( )
				.body( "total", equalTo( 1 ) )
				.and( )
				.body( "items[0].Content.Headers.From", contains( "sender@example.com" ) )
				.and( )
				.body( "items[0].Content.Headers.To", contains( "recipient@example.com" ) )
				.and( )
				.body( "items[0].Content.Headers.Subject", contains( "Test-Subject" ) )
				.and( )
				.body( "items[0].Content.Body", equalTo( "Test-Body" ) );

		mockMvc.perform( get( "/" ) )
				.andExpect( status( ).isOk( ) )
				.andExpect( content( ).string( containsString( "recipient@example.com" ) ) )
				.andExpect( content( ).string( containsString( "Test-Subject" ) ) )
				.andExpect( content( ).string( containsString( "Test-Body" ) ) );
	}

}
