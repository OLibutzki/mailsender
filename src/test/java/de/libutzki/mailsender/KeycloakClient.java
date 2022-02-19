package de.libutzki.mailsender;

import static java.util.Collections.singletonList;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

class KeycloakClient implements AutoCloseable{

	private final Keycloak keycloak;
	
	KeycloakClient(Keycloak keycloak) {
		this.keycloak = keycloak;
	}

	RealmClient createRealm(String name) {
		RealmRepresentation mailsenderRealm = new RealmRepresentation();
		mailsenderRealm.setId(name);
		mailsenderRealm.setRealm(name);
		mailsenderRealm.setDisplayName(name);
		mailsenderRealm.setEnabled(true);

		keycloak.realms().create(mailsenderRealm);
		return new RealmClient(name);
	}
	
	
	@Override
	public void close() {
		keycloak.close();
	}

	
	class RealmClient {
		RealmClient(String realmName) {
			super();
			this.realmName = realmName;
		}

		private final String realmName;
		
		void createClient(String clientName, String redirectURI) {
			ClientRepresentation mailsenderClient = new ClientRepresentation();
			mailsenderClient.setId(clientName);
			mailsenderClient.setClientId(clientName);
			mailsenderClient.setName(clientName);
			mailsenderClient.setRedirectUris(singletonList(redirectURI));
			
			RealmResource realm = keycloak.realm(realmName);
			realm.clients().create(mailsenderClient).close();
		}
	}
}
