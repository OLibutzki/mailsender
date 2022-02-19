package de.libutzki.mailsender.model;

public record SentMailDTO(
		String sender,
		String recipient, 
		String subject, 
		String body) {

}
