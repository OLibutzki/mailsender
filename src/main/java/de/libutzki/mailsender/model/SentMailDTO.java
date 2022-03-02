package de.libutzki.mailsender.model;

public record SentMailDTO(
		String recipient,
		String subject,
		String body ) {

}
