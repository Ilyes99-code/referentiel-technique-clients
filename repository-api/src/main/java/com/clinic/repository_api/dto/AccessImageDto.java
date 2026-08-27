package com.clinic.repository_api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata for one stored image. Carries no bytes — the payload is streamed from a
 * dedicated endpoint so that listing a client's gallery stays a cheap query.
 */
@Schema(description = "Métadonnées d'une image. Les octets se récupèrent séparément via "
        + "`GET /api/clients/{clientId}/access-images/{imageId}/content`.")
public record AccessImageDto(
        @Schema(description = "Identifiant de l'image", example = "1")
        Long id,

        @Schema(description = "Client propriétaire", example = "1")
        Long clientId,

        @Schema(description = "Nom d'origine du fichier, nettoyé des séparateurs de chemin "
                + "et des caractères de contrôle", example = "schema-reseau.png")
        String fileName,

        @Schema(description = "Type MIME déterminé à partir de la signature binaire du fichier, "
                + "et non de ce que le client a déclaré",
                example = "image/png",
                allowableValues = { "image/png", "image/jpeg", "image/webp", "image/gif" })
        String contentType,

        @Schema(description = "Taille en octets (5 242 880 maximum)", example = "2701373")
        long sizeBytes,

        @Schema(description = "Horodatage du téléversement (UTC, ISO-8601)",
                example = "2026-08-23T18:41:05.659Z")
        Instant uploadedAt
) {}
