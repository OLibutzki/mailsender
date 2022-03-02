package de.libutzki.mailsender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.model.SentMailDTO;

@SpringBootTest
@DirtiesContext
@TestPropertySource( properties = {
		"spring.datasource.url=jdbc:tc:postgresql:14.1:///testdb",
} )
public class Stage_03_MailServicePostgresTest {

	@Autowired
	private MailService mailService;

	@MockBean
	private JavaMailSender mailSender;

	@Test
	void test( ) {
		final NewMail newMail = new NewMail( "recipient@example.com", "Test-Subject", "Test-Body" );

		mailService.sendMail( "sender@example.com", newMail );

		final ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
		verify( mailSender ).send( mailMessageCaptor.capture( ) );

		assertThat( mailMessageCaptor.getValue( ) )
				.extracting(
						SimpleMailMessage::getFrom,
						SimpleMailMessage::getTo,
						SimpleMailMessage::getSubject,
						SimpleMailMessage::getText )
				.containsExactly(
						"sender@example.com",
						new String[] { "recipient@example.com" },
						"Test-Subject",
						"Test-Body" );

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
