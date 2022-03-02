package de.libutzki.mailsender.integration;

import static java.util.Collections.singletonList;

import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

class KeycloakClient implements AutoCloseable {

	private final Keycloak keycloak;

	KeycloakClient( final Keycloak keycloak ) {
		this.keycloak = keycloak;
	}

	RealmClient createRealm( final String name ) {
		final RealmRepresentation mailsenderRealm = new RealmRepresentation( );
		mailsenderRealm.setId( name );
		mailsenderRealm.setRealm( name );
		mailsenderRealm.setDisplayName( name );
		mailsenderRealm.setEnabled( true );

		keycloak.realms( ).create( mailsenderRealm );
		return new RealmClient( name );
	}

	@Override
	public void close( ) {
		keycloak.close( );
	}

	class RealmClient {
		RealmClient( final String realmName ) {
			super( );
			this.realmName = realmName;
		}

		private final String realmName;

		void createClient( final String clientName, final String redirectURI ) {
			final ClientRepresentation mailsenderClient = new ClientRepresentation( );
			mailsenderClient.setId( clientName );
			mailsenderClient.setClientId( clientName );
			mailsenderClient.setName( clientName );
			mailsenderClient.setRedirectUris( singletonList( redirectURI ) );
			mailsenderClient.setPublicClient( Boolean.TRUE );
			final RealmResource realm = keycloak.realm( realmName );
			realm.clients( ).create( mailsenderClient ).close( );
		}

		void createUser( final User userToCreate ) {

			final UserRepresentation user = new UserRepresentation( );
			user.setEnabled( true );
			user.setUsername( userToCreate.username( ) );
			user.setEmail( userToCreate.eMail( ) );
			user.setEmailVerified( true );
			final RealmResource realm = keycloak.realm( realmName );
			final UsersResource usersResource = realm.users( );
			final String userId;
			try ( Response createUserResponse = usersResource.create( user ) ) {
				userId = createUserResponse.getLocation( ).getPath( ).replaceAll( ".*/([^/]+)$", "$1" );
			}
			final UserResource userResource = usersResource.get( userId );
			final CredentialRepresentation passwordCred = new CredentialRepresentation( );
			passwordCred.setTemporary( false );
			passwordCred.setType( CredentialRepresentation.PASSWORD );
			passwordCred.setValue( userToCreate.password( ) );
			userResource.resetPassword( passwordCred );
		}
	}
}
