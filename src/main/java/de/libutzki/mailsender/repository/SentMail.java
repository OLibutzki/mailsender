package de.libutzki.mailsender.repository;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

@Entity
public class SentMail {

	@Id
	@GeneratedValue
	private Long id;

	@NotBlank
	private String sender;

	@NotBlank
	private String recipient;

	private String subject;

	private String body;

	public String getRecipient( ) {
		return recipient;
	}

	public void setRecipient( final String recipient ) {
		this.recipient = recipient;
	}

	public Optional<String> getSubject( ) {
		return Optional.ofNullable( subject );
	}

	public void setSubject( final String subject ) {
		this.subject = subject;
	}

	public Optional<String> getBody( ) {
		return Optional.ofNullable( body );
	}

	public void setBody( final String body ) {
		this.body = body;
	}

	public Long getId( ) {
		return id;
	}

	public String getSender( ) {
		return sender;
	}

	public void setSender( final String sender ) {
		this.sender = sender;
	}

	@Override
	public int hashCode( ) {
		return Objects.hash( id );
	}

	@Override
	public boolean equals( final Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( ( obj == null ) || ( getClass( ) != obj.getClass( ) ) ) {
			return false;
		}
		final SentMail other = ( SentMail ) obj;
		return Objects.equals( id, other.id );
	}

}
