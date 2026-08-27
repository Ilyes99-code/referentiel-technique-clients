package com.clinic.repository_api.controller;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.clinic.repository_api.dto.AccessImageContent;
import com.clinic.repository_api.dto.AccessImageDto;
import com.clinic.repository_api.service.AccessImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Images attached to a client's technical-access section.
 *
 * A separate controller rather than extra mappings on TechnicalAccessController:
 * that one owns /api/clients/{clientId}/access-techniques/{id}, and hanging a
 * literal /images segment off the same base would sit next to a {id} Long path
 * variable — legal, but it makes routing depend on pattern-specificity rules
 * instead of on distinct resources.
 */
@RestController
@RequestMapping("/api/clients/{clientId}/access-images")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Captures & visuels",
        description = """
                Images rattachées à la section « Accès techniques » d'un client : schémas réseau,
                copies d'écran de configuration VPN, règles de pare-feu.

                Les octets sont stockés en base (colonne `BYTEA`) et non sur disque, afin que la
                suppression d'un client emporte ses images et qu'une sauvegarde de la base suffise.

                **Formats acceptés :** PNG, JPEG, WEBP, GIF — 5 Mo par image, 40 images par client.
                Le type est déterminé côté serveur à partir de la signature binaire du fichier, et
                non du `Content-Type` annoncé par le client : un SVG renommé en `.png` est rejeté,
                puisqu'il pourrait être renvoyé plus tard avec un type que le navigateur exécute.""")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis", content = @Content()),
        @ApiResponse(responseCode = "404", description = "Client ou image introuvable", content = @Content())
})
public class AccessImageController {

    private final AccessImageService imageService;

    public AccessImageController(AccessImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "Lister les images d'un client",
            description = "Métadonnées uniquement, triées de la plus récente à la plus ancienne. "
                    + "Les octets ne sont jamais inclus ici : la requête ne lit même pas la colonne "
                    + "`BYTEA`, sans quoi afficher une grille de vignettes chargerait plusieurs "
                    + "mégaoctets par image en mémoire.")
    @GetMapping
    public List<AccessImageDto> findAll(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId) {
        return imageService.findAllByClient(clientId);
    }

    @Operation(summary = "Téléverser une image",
            description = "Requête `multipart/form-data` avec une partie nommée `file`.\n\n"
                    + "Le format réel est déduit des octets du fichier ; le `Content-Type` déclaré est ignoré.")
    @ApiResponse(responseCode = "201", description = "Image enregistrée")
    @ApiResponse(responseCode = "400",
            description = "Format non pris en charge, fichier vide, ou limite de 40 images atteinte",
            content = @Content())
    @ApiResponse(responseCode = "413", description = "Fichier supérieur à 5 Mo", content = @Content())
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AccessImageDto upload(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Fichier image (PNG, JPEG, WEBP ou GIF — 5 Mo maximum)")
            @RequestParam("file") MultipartFile file) {
        return imageService.upload(clientId, file);
    }

    /**
     * Streams the raw bytes. Kept behind the same ADMIN check as everything else, which
     * means a browser cannot load it through a plain <img src> — an image element sends
     * no Authorization header. The frontend therefore fetches this with the bearer token
     * and renders the result through a blob: URL. The alternative (a public or
     * token-in-query-string URL) would put client infrastructure screenshots one leaked
     * referrer or proxy log away from disclosure.
     *
     * X-Content-Type-Options: nosniff is already applied to every response by Spring
     * Security's default header writers, so it is not set again here.
     */
    @Operation(summary = "Télécharger les octets d'une image",
            description = """
                    Renvoie le fichier brut avec son `Content-Type` d'origine.

                    Cette route est authentifiée comme les autres, ce qui a une conséquence directe
                    côté navigateur : une balise `<img src="…">` n'envoie pas d'en-tête
                    `Authorization` et recevra donc un 401. Le frontend récupère les octets via
                    `fetch()` avec le jeton, puis les affiche par une URL `blob:`.

                    L'alternative — rendre la route publique ou passer le jeton en paramètre d'URL —
                    exposerait des captures d'infrastructure client au premier journal de proxy ou
                    en-tête `Referer` venu.""")
    @ApiResponse(responseCode = "200", description = "Octets de l'image",
            content = @Content(mediaType = "image/*",
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/{imageId}/content")
    public ResponseEntity<byte[]> content(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant de l'image", example = "1") @PathVariable Long imageId) {
        AccessImageContent image = imageService.loadContent(clientId, imageId);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(image.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(image.data());
    }

    @Operation(summary = "Supprimer une image", description = "Suppression définitive, sans corbeille.")
    @ApiResponse(responseCode = "204", description = "Image supprimée")
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant de l'image", example = "1") @PathVariable Long imageId) {
        imageService.delete(clientId, imageId);
    }
}
