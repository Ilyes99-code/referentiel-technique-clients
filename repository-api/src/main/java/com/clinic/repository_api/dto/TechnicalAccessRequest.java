package com.clinic.repository_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Corps de création/mise à jour d'un accès technique")
public record TechnicalAccessRequest(
        @Schema(description = "Type d'accès. Texte libre ; l'interface utilise « LogMeIn », "
                + "« SQL Server », « Admin Access » et « VPN Accès ».",
                example = "VPN Accès", maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le type d'accès est requis")
        @Size(max = 50, message = "Le type d'accès ne doit pas dépasser 50 caractères")
        String type,

        @Schema(description = "Libellé descriptif", example = "Tunnel principal", maxLength = 255)
        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
        String description,

        @Schema(description = "Hôte, IP ou instance", example = "vpn.clinique-ibnsina.tn", maxLength = 255)
        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String address,

        @Schema(description = "Port réseau, ou `null` si non pertinent", example = "1194",
                minimum = "1", maximum = "65535")
        @Min(value = 1, message = "Le port doit être compris entre 1 et 65535")
        @Max(value = 65535, message = "Le port doit être compris entre 1 et 65535")
        Integer port,

        @Schema(description = "Compte utilisé pour se connecter", example = "admin", maxLength = 100)
        @Size(max = 100, message = "L'utilisateur ne doit pas dépasser 100 caractères")
        String username,

        @Schema(description = "Mot de passe en clair. Chiffré en AES-256-GCM avant écriture en base. "
                + "Une chaîne vide efface le mot de passe stocké.", example = "Vpn#2026!")
        String password,

        @Schema(description = "Notes libres", example = "Renouveler le certificat en janvier.")
        String notes
) {}
