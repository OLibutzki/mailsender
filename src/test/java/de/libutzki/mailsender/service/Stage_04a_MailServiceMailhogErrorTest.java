package de.libutzki.mailsender.service;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import de.libutzki.mailsender.model.NewMail;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@SpringBootTest
@Testcontainers
@DirtiesContext
@TestPropertySource( properties = {
		"spring.datasource.url=jdbc:tc:postgresql:14.1:///Stage_04a_MailServiceMailhogErrorTest",
} )
public class Stage_04a_MailServiceMailhogErrorTest {

	@Autowired
	private MailService mailService;

	private static final Integer MAILHOG_SMTP_PORT = 1025;
	private static final Integer MAILHOG_HTTP_PORT = 8025;

	private static Network network = Network.newNetwork( );

	@Container
	static GenericContainer<?> mailhogContainer = new GenericContainer<>( "mailhog/mailhog:v1.0.1" )
			.withExposedPorts( MAILHOG_SMTP_PORT, MAILHOG_HTTP_PORT )
			.withNetwork( network )
			.waitingFor(
					Wait
							.forHttp( "/" )
							.forPort( MAILHOG_HTTP_PORT ) );

	@Container
	static ToxiproxyContainer toxiproxyContainer = new ToxiproxyContainer( DockerImageName.parse( "shopify/toxiproxy" ) )
			.withNetwork( network );

	private static ToxiproxyContainer.ContainerProxy proxy;

	@DynamicPropertySource
	static void configureMail( final DynamicPropertyRegistry registry ) {

		proxy = toxiproxyContainer.getProxy( mailhogContainer, MAILHOG_SMTP_PORT );
		proxy.setConnectionCut( true );
		registry.add( "spring.mail.host", proxy::getContainerIpAddress );
		registry.add( "spring.mail.port", proxy::getProxyPort );
	}

	private RequestSpecification mailhogRequestSpec;

	@BeforeEach
	void setupRestAssured( ) {

		System.out.println( "Mailhog: " + getClass( ).getName( ) + ": " + String.format( "http://%s", mailhogContainer.getHost( ) ) + "; " + mailhogContainer.getMappedPort( MAILHOG_HTTP_PORT ) + "; " + mailhogContainer.getMappedPort( MAILHOG_SMTP_PORT ) );
		mailhogRequestSpec = new RequestSpecBuilder( )
				.setBaseUri( String.format( "http://%s", mailhogContainer.getHost( ) ) )
				.setPort( mailhogContainer.getMappedPort( MAILHOG_HTTP_PORT ) )
				.setBasePath( "/api/v2" )
				.build( );
	}

	@Test
	void test( ) throws IOException {

		given( mailhogRequestSpec )
				.when( ).get( "/messages" )
				.then( ).body( "total", equalTo( 0 ) );

		final NewMail newMail = new NewMail( "recipient@example.com", "Test-Subject", "Test-Body" );

		assertThatExceptionOfType( MailSendException.class )
				.isThrownBy( ( ) -> mailService.sendMail( "sender@example.com", newMail ) );

	}
}
