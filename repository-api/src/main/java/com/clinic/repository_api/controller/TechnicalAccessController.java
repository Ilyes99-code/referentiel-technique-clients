package com.clinic.repository_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clinic.repository_api.dto.TechnicalAccessDto;
import com.clinic.repository_api.dto.TechnicalAccessRequest;
import com.clinic.repository_api.service.TechnicalAccessService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients/{clientId}/access-techniques")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Accès techniques",
        description = """
                Coffre d'identifiants : VPN, RDP, SQL Server, comptes administrateur.

                **Le champ `password` circule en clair.** Il est chiffré au repos en
                AES-256-GCM (`EncryptedStringConverter`) et déchiffré à la lecture — c'est le
                comportement attendu d'un coffre destiné aux administrateurs. En conséquence,
                les réponses de ces routes ne doivent jamais être journalisées, mises en cache
                par un intermédiaire, ni exposées à un rôle non-administrateur.""")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis", content = @Content()),
        @ApiResponse(responseCode = "404", description = "Client introuvable", content = @Content())
})
public class TechnicalAccessController {

    private final TechnicalAccessService accessService;

    public TechnicalAccessController(TechnicalAccessService accessService) {
        this.accessService = accessService;
    }

    @Operation(summary = "Lister les accès techniques d'un client",
            description = "Triés par identifiant croissant. Les mots de passe sont déchiffrés dans la réponse.")
    @GetMapping
    public List<TechnicalAccessDto> findAll(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId) {
        return accessService.findAllByClient(clientId);
    }

    @Operation(summary = "Récupérer un accès technique",
            description = "Recherché dans le périmètre du client : un identifiant appartenant à un autre "
                    + "client renvoie 404, afin de ne pas confirmer son existence.")
    @GetMapping("/{id}")
    public TechnicalAccessDto findById(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant de l'accès", example = "1") @PathVariable Long id) {
        return accessService.findById(clientId, id);
    }

    @Operation(summary = "Créer un accès technique",
            description = "`type` est un texte libre ; l'interface utilise « LogMeIn », « SQL Server », "
                    + "« Admin Access » et « VPN Accès ». `port` doit être compris entre 1 et 65535 s'il est fourni.")
    @ApiResponse(responseCode = "201", description = "Accès créé")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TechnicalAccessDto create(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Valid @RequestBody TechnicalAccessRequest request) {
        return accessService.create(clientId, request);
    }

    @Operation(summary = "Mettre à jour un accès technique",
            description = "Remplacement complet. Envoyer `password` vide efface le mot de passe stocké.")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @PutMapping("/{id}")
    public TechnicalAccessDto update(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant de l'accès", example = "1") @PathVariable Long id,
            @Valid @RequestBody TechnicalAccessRequest request) {
        return accessService.update(clientId, id, request);
    }

    @Operation(summary = "Supprimer un accès technique")
    @ApiResponse(responseCode = "204", description = "Accès supprimé")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant de l'accès", example = "1") @PathVariable Long id) {
        accessService.delete(clientId, id);
    }
}
