package com.clinic.repository_api.dto;

import com.clinic.repository_api.model.enums.ClientStatut;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fiche client renvoyée par l'API")
public record ClientDto(
        @Schema(description = "Identifiant attribué par le serveur", example = "1")
        Long id,

        @Schema(description = "Raison sociale", example = "Clinique Ibn Sina")
        String nom,

        @Schema(description = "Statut contractuel", example = "en regle")
        ClientStatut statut,

        @Schema(description = "Notes libres", example = "Migration SQL Server prévue Q4.")
        String notes
) {}
