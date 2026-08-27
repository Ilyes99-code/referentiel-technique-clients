package com.clinic.repository_api.controller;

import com.clinic.repository_api.dto.AuthResponse;
import com.clinic.repository_api.dto.LoginRequest;
import com.clinic.repository_api.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Obtention du jeton JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * The empty @SecurityRequirements cancels the document-level bearer requirement
     * declared in OpenApiConfig — without it, Swagger UI would show a padlock on the
     * one endpoint you must be able to call before you have a token.
     */
    @Operation(
            summary = "Se connecter",
            description = """
                    Vérifie les identifiants et renvoie un JWT HS256 valable 15 minutes.

                    Le jeton se place ensuite dans l'en-tête `Authorization: Bearer <token>`
                    de toutes les autres requêtes, ou dans le bouton **Authorize** de cette page.

                    Les tentatives échouées sont comptées par identifiant : au-delà du seuil,
                    le compte est temporairement verrouillé et renvoie `429`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie"),
            @ApiResponse(responseCode = "400", description = "Identifiant ou mot de passe absent", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides", content = @io.swagger.v3.oas.annotations.media.Content()),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives — compte temporairement verrouillé", content = @io.swagger.v3.oas.annotations.media.Content())
    })
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }
}
