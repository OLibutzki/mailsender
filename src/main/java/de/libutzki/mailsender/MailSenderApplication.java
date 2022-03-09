package de.libutzki.mailsender;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource( value = "classpath:/application.dev.properties", ignoreResourceNotFound = true )
public class MailSenderApplication {

	public static void main( String[] args ) {
		createSpringApplication( ).run( args );
	}

	public static SpringApplication createSpringApplication( ) {
		return new SpringApplication( MailSenderApplication.class );
	}

	@Bean
	public KeycloakConfigResolver KeycloakConfigResolver( ) {
		return new KeycloakSpringBootConfigResolver( );
	}

}
