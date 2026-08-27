package com.clinic.repository_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Identifiants de connexion")
public record LoginRequest(
        @Schema(description = "Nom d'utilisateur", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String username,

        @Schema(description = "Mot de passe en clair (transporté par HTTPS en production)",
                example = "Admin@2024!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200) String password
) {}
