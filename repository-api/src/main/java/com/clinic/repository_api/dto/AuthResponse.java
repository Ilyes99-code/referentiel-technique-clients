package com.clinic.repository_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jeton d'accès et identité de l'utilisateur connecté")
public record AuthResponse(
        @Schema(description = "JWT signé en HS256, valable 15 minutes. À placer dans "
                + "`Authorization: Bearer <token>`. Aucun refresh token n'est émis.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9.…")
        String accessToken,

        @Schema(description = "Nom d'utilisateur authentifié", example = "admin")
        String username,

        @Schema(description = "Rôle de l'utilisateur — seul ADMIN existe aujourd'hui", example = "ADMIN")
        String role
) {}
