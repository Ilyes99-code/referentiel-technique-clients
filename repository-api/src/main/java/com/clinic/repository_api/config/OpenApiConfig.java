package com.clinic.repository_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI 3 document metadata.
 *
 * springdoc discovers the endpoints, DTO shapes and Bean Validation constraints by
 * reflection on its own; what it cannot infer is the prose, the auth model, and
 * which servers the spec applies to. That is what this class supplies.
 *
 * Availability is controlled by springdoc.* properties per profile rather than here:
 * enabled in dev, opt-in via SPRINGDOC_ENABLED in prod. See application-prod.properties.
 */
@Configuration
public class OpenApiConfig {

    /** Name of the scheme referenced by @SecurityRequirement and the "Authorize" button. */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI referentielOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Référentiel Technique Clients — API")
                        .version("v1")
                        .description("""
                                API interne du référentiel technique clients : fiches clients, modules
                                installés, accès techniques (identifiants chiffrés) et captures associées.

                                ## Authentification

                                Toutes les routes sont protégées **sauf** `POST /api/auth/login`,
                                `/actuator/health` et `/error`. Le rôle `ADMIN` est requis partout ailleurs.

                                1. Appelez `POST /api/auth/login` avec un identifiant et un mot de passe.
                                2. Copiez le champ `accessToken` de la réponse.
                                3. Cliquez sur **Authorize** (en haut à droite) et collez le jeton.

                                Le jeton est un JWT signé en HS256, valable **15 minutes**. Il n'y a pas de
                                refresh token : une fois expiré, il faut se reconnecter. Une requête
                                authentifiée rejetée renvoie `401`, un rôle insuffisant renvoie `403`.

                                ## Format des erreurs

                                Toutes les erreurs suivent la même forme JSON, produite par
                                `GlobalExceptionHandler` :

                                ```json
                                { "message": "Client introuvable: 42" }
                                ```

                                Les erreurs de validation ajoutent le détail par champ :

                                ```json
                                { "message": "Validation échouée", "errors": { "nom": "Le nom du client est requis" } }
                                ```

                                ## Note sur les données sensibles

                                Les mots de passe des accès techniques sont chiffrés au repos (AES-256-GCM)
                                mais **renvoyés en clair** aux administrateurs authentifiés : c'est le
                                comportement attendu d'un coffre d'identifiants. Les réponses de cette API
                                ne doivent donc jamais être journalisées ni mises en cache par un
                                intermédiaire.
                                """)
                        .contact(new Contact().name("Équipe Référentiel Technique"))
                        .license(new License().name("Usage interne")))
                // No .servers(...) on purpose. Hardcoding one would be wrong as soon as the
                // port differs between the container and the host — the API listens on 8082
                // inside its container but is published on ${BACKEND_PORT} (8083 locally),
                // and behind nginx it is reached on the frontend's origin entirely. Left
                // unset, springdoc derives the server URL from the incoming request, so
                // "Try it out" always targets whatever origin actually served this page.
                //
                // Applied at the document level so every operation inherits it; the login
                // endpoint opts back out with @SecurityRequirements (plural, empty).
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Collez uniquement le jeton — Swagger UI ajoute « Bearer » lui-même.")));
    }
}
