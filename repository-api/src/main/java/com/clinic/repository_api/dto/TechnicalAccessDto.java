package com.clinic.repository_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Entrée du coffre d'identifiants d'un client")
public record TechnicalAccessDto(
        @Schema(description = "Identifiant de l'accès", example = "1")
        Long id,

        @Schema(description = "Client propriétaire", example = "1")
        Long clientId,

        @Schema(description = "Type d'accès (texte libre)", example = "VPN Accès")
        String type,

        @Schema(description = "Libellé descriptif", example = "Tunnel principal")
        String description,

        @Schema(description = "Hôte, IP ou instance", example = "vpn.clinique-ibnsina.tn")
        String address,

        @Schema(description = "Port réseau, si pertinent", example = "1194")
        Integer port,

        @Schema(description = "Compte utilisé pour se connecter", example = "admin")
        String username,

        @Schema(description = "Mot de passe **en clair**. Chiffré en AES-256-GCM au repos et "
                + "déchiffré à la lecture : c'est la raison d'être d'un coffre d'identifiants. "
                + "Ne pas journaliser ni mettre en cache les réponses contenant ce champ.",
                example = "Vpn#2026!")
        String password,

        @Schema(description = "Notes libres", example = "Renouveler le certificat en janvier.")
        String notes
) {}
