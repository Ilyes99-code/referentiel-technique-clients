package com.clinic.repository_api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Module applicatif installé chez un client")
public record ModuleDto(
        @Schema(description = "Identifiant du module", example = "1")
        Long id,

        @Schema(description = "Client propriétaire", example = "1")
        Long clientId,

        @Schema(description = "Nom du module", example = "DMI Web")
        String module,

        @Schema(description = "Version déployée", example = "3.4.0")
        String version,

        @Schema(description = "Date de dernière mise à jour (ISO `AAAA-MM-JJ`)", example = "2026-06-14")
        LocalDate dateMaj,

        @Schema(description = "Date de mise en production (ISO `AAAA-MM-JJ`)", example = "2025-11-02")
        LocalDate dateMep,

        @Schema(description = "URL d'accès externe", example = "https://dmi.clinique-ibnsina.tn")
        String lienExterne,

        @Schema(description = "URL d'accès interne", example = "http://192.168.1.40:8080")
        String lienInterne
) {}
