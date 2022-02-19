package de.libutzki.mailsender.web;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.service.MailService;

@Controller
@RequestMapping("/")
public class MailController {

	private static final String SENDER = "oliver@libutzki.de";

	private final MailService mailService;

	public MailController(MailService mailService) {
		this.mailService = mailService;
	}

	@GetMapping
	public String index(Model model) {
		addAttributes(model);
		return "index";
	}
	
	@GetMapping("logout-successful")
	public String logoutSuccessful() {
		return "redirect:/";
	}

	@PostMapping
	public String sendMail(@Valid @ModelAttribute("mail") NewMail mail, Model model) {
		mailService.sendMail(SENDER, mail);

		addAttributes(model);
		return "redirect:/";
	}

	private void addAttributes(Model model) {
		model.addAttribute("sentMails", mailService.getSentMailsForSender(SENDER));
	}

}
