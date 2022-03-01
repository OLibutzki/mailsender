package de.libutzki.mailsender.service;

import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.model.SentMailDTO;
import de.libutzki.mailsender.repository.SentMail;
import de.libutzki.mailsender.repository.SentMailRepository;

@Service
public class MailService {

	private final JavaMailSender mailSender;
	private final SentMailRepository sentMailRepository;

	public MailService( final JavaMailSender mailSender, final SentMailRepository sentMailRepository ) {
		this.mailSender = mailSender;
		this.sentMailRepository = sentMailRepository;
	}

	@Transactional( rollbackFor = Exception.class )
	public void sendMail( final String sender, final NewMail newMail ) {
		final SimpleMailMessage message = new SimpleMailMessage( );
		message.setFrom( sender );
		message.setTo( newMail.recipient( ) );
		message.setSubject( newMail.subject( ) );
		message.setText( newMail.body( ) );
		mailSender.send( message );

		final SentMail sentMail = new SentMail( );
		sentMail.setSender( sender );
		sentMail.setRecipient( newMail.recipient( ) );
		sentMail.setSubject( newMail.subject( ) );
		sentMail.setBody( newMail.body( ) );

		sentMailRepository.save( sentMail );
	}

	@Transactional( readOnly = true )
	public List<SentMailDTO> getSentMailsForSender( final String sender ) {
		return sentMailRepository.findBySender( sender );
	}
}
