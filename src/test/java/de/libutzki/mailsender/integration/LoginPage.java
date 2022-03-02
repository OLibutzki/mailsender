package de.libutzki.mailsender.integration;

import com.microsoft.playwright.Page;

final class LoginPage {

	private final Page page;

	public LoginPage( Page page ) {
		this.page = page;
	}

	public void login( String username, String password ) {
		page.locator( "id=username" ).fill( username );
		page.locator( "id=password" ).fill( password );
		page.locator( "id=kc-login" ).click( );
	}
}
