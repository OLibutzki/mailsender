package de.libutzki.mailsender.development;

import java.util.Map;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import dasniko.testcontainers.keycloak.KeycloakContainer;

class DevEnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>( "postgres:14.1" )
			.withReuse( true );

	static KeycloakContainer keycloakContainer = new KeycloakContainer( "quay.io/keycloak/keycloak:18.0.2" )
			.withReuse( true );

	private static final Integer MAILHOG_SMTP_PORT = 1025;
	private static final Integer MAILHOG_HTTP_PORT = 8025;

	static GenericContainer<?> mailhogContainer = new GenericContainer<>( "mailhog/mailhog:v1.0.1" )
			.withExposedPorts( MAILHOG_SMTP_PORT, MAILHOG_HTTP_PORT )
			.withReuse( true )
			.waitingFor(
					Wait
							.forHttp( "/" )
							.forPort( MAILHOG_HTTP_PORT ) );

	private static String getJdbcUrl( ) {
		return "jdbc:postgresql://" + postgresContainer.getContainerIpAddress( ) + ":" + postgresContainer.getMappedPort( PostgreSQLContainer.POSTGRESQL_PORT )
				+ "/" + postgresContainer.getDatabaseName( );
	}

	public static Map<String, Object> getProperties( ) {
		Startables.deepStart( Stream.of( postgresContainer, keycloakContainer, mailhogContainer ) ).join( );

		System.out.println( String.format( "Postgres running at: %s(%s:%s", getJdbcUrl( ), postgresContainer.getUsername( ), postgresContainer.getPassword( ) ) );
		System.out.println( String.format( "Keycloak running at: %s", keycloakContainer.getAuthServerUrl( ) ) );
		System.out.println( String.format( "Mailhog running at: http://%s:%s/", mailhogContainer.getHost( ), mailhogContainer.getMappedPort( MAILHOG_HTTP_PORT ) ) );

		return Map.of(
				"spring.datasource.url", postgresContainer.getJdbcUrl( ),
				"spring.datasource.username", postgresContainer.getUsername( ),
				"spring.datasource.password", postgresContainer.getPassword( ),

				"keycloak.auth-server-url", keycloakContainer.getAuthServerUrl( ),
				"spring.mail.host", mailhogContainer.getHost( ),
				"spring.mail.port", mailhogContainer.getMappedPort( MAILHOG_SMTP_PORT ) );
	}

	@Override
	public void initialize( ConfigurableApplicationContext context ) {
		context.getEnvironment( ).getPropertySources( ).addFirst( new MapPropertySource(
				"testcontainers",
				getProperties( ) ) );
	}
}