package com.clinic.repository_api.dto;

import com.clinic.repository_api.model.enums.ClientStatut;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = """
        Corps de création/mise à jour d'un client.

        Notez l'absence de `id` : l'identifiant est toujours attribué par le serveur. L'accepter
        dans le corps de la requête permettrait à un appelant de désigner la ligne à écrire.""")
public record ClientRequest(
        @Schema(description = "Raison sociale du client", example = "Clinique Ibn Sina",
                maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le nom du client est requis")
        @Size(max = 255, message = "Le nom du client ne doit pas dépasser 255 caractères")
        String nom,

        @Schema(description = "Statut contractuel. Omis, vaut `en regle`.", example = "en regle")
        ClientStatut statut,  // nullable — defaults to EN_REGLE in service

        @Schema(description = "Notes libres sur le client", example = "Migration SQL Server prévue Q4.")
        String notes          // nullable — free-text notes for this client
) {}
