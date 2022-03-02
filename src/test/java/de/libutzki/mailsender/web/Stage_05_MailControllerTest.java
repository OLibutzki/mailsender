package de.libutzki.mailsender.web;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.keycloak.WithMockKeycloakAuth;

import de.libutzki.mailsender.model.NewMail;
import de.libutzki.mailsender.model.SentMailDTO;
import de.libutzki.mailsender.service.MailService;

@SpringBootTest( webEnvironment = WebEnvironment.RANDOM_PORT )
@AutoConfigureMockMvc
// Disable persistence
@TestPropertySource( properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration, org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration" )
class Stage_05_MailControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private MailService mailService;

	@Test
	@WithMockKeycloakAuth( claims = @OpenIdClaims( email = "sender@example.com" ) )
	public void testWithAuth( ) throws Exception {

		when( mailService.getSentMailsForSender( eq( "sender@example.com" ) ) ).thenReturn(
				singletonList(
						new SentMailDTO( "recipient@example.com", "Test-Subject", "Test-Body" ) ) );

		this.mockMvc.perform( get( "/" ) )
				.andExpect( status( ).isOk( ) )
				.andExpect( content( ).string( containsString( "recipient@example.com" ) ) )
				.andExpect( content( ).string( containsString( "Test-Subject" ) ) )
				.andExpect( content( ).string( containsString( "Test-Body" ) ) );
	}

	@Test
	public void testWithoutAuth( ) throws Exception {

		when( mailService.getSentMailsForSender( eq( "sender@example.com" ) ) ).thenReturn(
				singletonList(
						new SentMailDTO( "recipient@example.com", "Test-Subject", "Test-Body" ) ) );

		this.mockMvc.perform( get( "/" ) )
				.andExpect( status( ).is3xxRedirection( ) );
	}

	@Test
	@WithMockKeycloakAuth( claims = @OpenIdClaims( email = "sender@example.com" ) )
	public void testSendMail( ) throws Exception {

		this.mockMvc.perform( post( "/" )
				.contentType( MediaType.APPLICATION_FORM_URLENCODED )
				.with( csrf( ) )
				.param( "recipient", "recipient@example.com" )
				.param( "subject", "Test-Subject" )
				.param( "body", "Test-Body" ) )

				.andExpect( status( ).is3xxRedirection( ) );

		verify( mailService )
				.sendMail(
						eq( "sender@example.com" ),
						eq( new NewMail( "recipient@example.com", "Test-Subject", "Test-Body" ) ) );
	}

}
