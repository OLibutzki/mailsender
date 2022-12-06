package de.libutzki.mailsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication
@PropertySources( {
		@PropertySource( value = "classpath:/app.properties" ),
		@PropertySource( value = "classpath:/app.dev.properties", ignoreResourceNotFound = true ),

} )
public class MailSenderApplication {

	public static void main( String[] args ) {
		createSpringApplication( ).run( args );
	}

	public static SpringApplication createSpringApplication( ) {
		return new SpringApplication( MailSenderApplication.class );
	}

}
