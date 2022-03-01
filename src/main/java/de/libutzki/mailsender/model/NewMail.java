package de.libutzki.mailsender.model;

public record NewMail(
		String recipient,
		String subject,
		String body ) {
}
