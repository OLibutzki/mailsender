package de.libutzki.mailsender.service;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.model.SentMailDTO;
import io.restassured.RestAssured;

@SpringBootTest
@Testcontainers
@DirtiesContext
@TestPropertySource( properties = {
		"spring.datasource.url=jdbc:tc:postgresql:14.1:///testdb",
} )
public class Stage_04_MailServiceMailhogTest {

	@Autowired
	private MailService mailService;

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
	void test( ) {

		given( )
				.when( ).get( "/messages" )
				.then( ).body( "total", equalTo( 0 ) );

		final NewMail newMail = new NewMail( "recipient@example.com", "Test-Subject", "Test-Body" );

		mailService.sendMail( "sender@example.com", newMail );

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

		final List<SentMailDTO> actualSentMails = mailService.getSentMailsForSender( "sender@example.com" );

		assertThat( actualSentMails )
				.extracting(
						SentMailDTO::recipient,
						SentMailDTO::subject,
						SentMailDTO::body )
				.containsExactly(
						tuple(
								"recipient@example.com",
								"Test-Subject",
								"Test-Body" ) );

	}
}
