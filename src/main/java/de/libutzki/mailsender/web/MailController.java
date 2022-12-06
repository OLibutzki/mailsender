package de.libutzki.mailsender.web;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.service.MailService;
import jakarta.validation.Valid;

@Controller
@RequestMapping( "/" )
public class MailController {

	private final MailService mailService;

	public MailController( final MailService mailService ) {
		this.mailService = mailService;
	}

	@GetMapping
	public String index( final Model model, final Principal principal ) {
		addAttributes( model, getEMailAddress( principal ) );
		return "index";
	}

	@GetMapping( "logout-successful" )
	public String logoutSuccessful( ) {
		return "redirect:/";
	}

	@PostMapping
	public String sendMail( @Valid @ModelAttribute( "mail" ) final NewMail mail, final Model model, final Principal principal ) {
		final String senderEMailAddress = getEMailAddress( principal );
		mailService.sendMail( senderEMailAddress, mail );

		addAttributes( model, senderEMailAddress );
		return "redirect:/";
	}

	private void addAttributes( final Model model, final String senderEMailAddress ) {
		model.addAttribute( "sentMails", mailService.getSentMailsForSender( senderEMailAddress ) );
	}

	private String getEMailAddress( final Principal principal ) {
		// final KeycloakAuthenticationToken authToken = ( KeycloakAuthenticationToken ) principal;
		// final String email = authToken.getAccount( ).getKeycloakSecurityContext( ).getToken( ).getEmail( );
		// return email;
		return "example@example.com";
	}

}
