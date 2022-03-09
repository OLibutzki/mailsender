package de.libutzki.mailsender.development;

import de.libutzki.mailsender.MailSenderApplication;

public class MailSenderWithDevEnvironment {
	public static void main( String[] args ) {
		final var application = MailSenderApplication.createSpringApplication( );

		// Here we add the same initializer as we were using in our tests...
		application.addInitializers( new DevEnvironmentInitializer( ) );

		// ... and start it normally
		application.run( args );
	}
}
