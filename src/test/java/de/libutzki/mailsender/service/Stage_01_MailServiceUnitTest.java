package de.libutzki.mailsender.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.model.SentMailDTO;
import de.libutzki.mailsender.repository.SentMail;
import de.libutzki.mailsender.repository.SentMailRepository;

class Stage_01_MailServiceUnitTest {

	private JavaMailSender mailSender;
	private SentMailRepository sentMailRepository;
	private MailService mailService;

	@BeforeEach
	void init( ) {
		mailSender = mock( JavaMailSender.class );
		sentMailRepository = mock( SentMailRepository.class );
		mailService = new MailService( mailSender, sentMailRepository );
	}

	@Test
	void testSendMail( ) {

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

		final SentMail sentMail = new SentMail( );
		sentMail.setSender( "sender@example.com" );
		sentMail.setRecipient( "recipient@example.com" );
		sentMail.setSubject( "Test-Subject" );
		sentMail.setBody( "Test-Body" );

		final ArgumentCaptor<SentMail> sentMailCaptor = ArgumentCaptor.forClass( SentMail.class );
		verify( sentMailRepository ).save( sentMailCaptor.capture( ) );

		assertThat( sentMailCaptor.getValue( ) )
				.extracting(
						SentMail::getSender,
						SentMail::getRecipient,
						SentMail::getSubject,
						SentMail::getBody )
				.containsExactly(
						"sender@example.com",
						"recipient@example.com",
						Optional.of( "Test-Subject" ),
						Optional.of( "Test-Body" ) );

	}

	@Test
	void testGetSentMailsForSender( ) {

		final SentMailDTO sentMailDTO = new SentMailDTO( "recipient@example.com", "Test-Subject", "Test-Body" );

		when( sentMailRepository.findBySender( anyString( ) ) )
				.thenReturn( emptyList( ) );

		when( sentMailRepository.findBySender( "sender@example.com" ) )
				.thenReturn( singletonList( sentMailDTO ) );

		final List<SentMailDTO> actualSentMailsForSender = mailService.getSentMailsForSender( "sender@example.com" );

		assertThat( actualSentMailsForSender )
				.containsExactly( sentMailDTO );

		final List<SentMailDTO> actualSentMailsForSender2 = mailService.getSentMailsForSender( "sender2@example.com" );

		assertThat( actualSentMailsForSender2 ).isEmpty( );
	}
}
