package de.libutzki.mailsender.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import de.libutzki.mailsender.model.SentMailDTO;

public interface SentMailRepository extends JpaRepository<SentMail, Long> {

	@Query("SELECT new de.libutzki.mailsender.model.SentMailDTO(mail.sender, mail.recipient, mail.subject, mail.body) FROM SentMail mail WHERE mail.sender = :sender ORDER BY mail.id")
	List<SentMailDTO> findBySender(String sender);
}
