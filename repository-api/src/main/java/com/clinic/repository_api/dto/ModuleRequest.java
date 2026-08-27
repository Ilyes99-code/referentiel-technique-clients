package com.clinic.repository_api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Corps de création/mise à jour d'un module")
public record ModuleRequest(
        @Schema(description = "Nom du module", example = "DMI Web",
                maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le nom du module est requis")
        @Size(max = 255, message = "Le nom du module ne doit pas dépasser 255 caractères")
        String module,

        @Schema(description = "Version déployée", example = "3.4.0", maxLength = 100)
        @Size(max = 100, message = "La version ne doit pas dépasser 100 caractères")
        String version,

        @Schema(description = "Date de dernière mise à jour (ISO `AAAA-MM-JJ`), ou `null`",
                example = "2026-06-14")
        LocalDate dateMaj,

        @Schema(description = "Date de mise en production (ISO `AAAA-MM-JJ`), ou `null`",
                example = "2025-11-02")
        LocalDate dateMep,

        @Schema(description = "URL d'accès externe", example = "https://dmi.clinique-ibnsina.tn", maxLength = 500)
        @Size(max = 500, message = "Le lien externe ne doit pas dépasser 500 caractères")
        String lienExterne,

        @Schema(description = "URL d'accès interne", example = "http://192.168.1.40:8080", maxLength = 500)
        @Size(max = 500, message = "Le lien interne ne doit pas dépasser 500 caractères")
        String lienInterne
) {}
