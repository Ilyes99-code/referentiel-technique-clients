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

import com.clinic.repository_api.dto.ModuleDto;
import com.clinic.repository_api.dto.ModuleRequest;
import com.clinic.repository_api.service.ClientModuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients/{clientId}/modules")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Modules", description = "Modules applicatifs installés chez un client")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis", content = @Content()),
        @ApiResponse(responseCode = "404", description = "Client introuvable", content = @Content())
})
public class ClientModuleController {

    private final ClientModuleService moduleService;

    public ClientModuleController(ClientModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @Operation(summary = "Lister les modules d'un client")
    @GetMapping
    public List<ModuleDto> findAll(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId) {
        return moduleService.findAllByClient(clientId);
    }

    @Operation(summary = "Récupérer un module",
            description = "Le module est recherché dans le périmètre du client : un identifiant valide "
                    + "appartenant à un autre client renvoie 404 plutôt que la fiche.")
    @GetMapping("/{id}")
    public ModuleDto findById(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant du module", example = "1") @PathVariable Long id) {
        return moduleService.findById(clientId, id);
    }

    @Operation(summary = "Ajouter un module à un client",
            description = "`dateMaj` et `dateMep` sont des dates ISO (`AAAA-MM-JJ`) et peuvent être nulles.")
    @ApiResponse(responseCode = "201", description = "Module créé")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleDto create(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Valid @RequestBody ModuleRequest request) {
        return moduleService.create(clientId, request);
    }

    @Operation(summary = "Mettre à jour un module", description = "Remplacement complet de la fiche module.")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @PutMapping("/{id}")
    public ModuleDto update(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant du module", example = "1") @PathVariable Long id,
            @Valid @RequestBody ModuleRequest request) {
        return moduleService.update(clientId, id, request);
    }

    @Operation(summary = "Supprimer un module")
    @ApiResponse(responseCode = "204", description = "Module supprimé")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long clientId,
            @Parameter(description = "Identifiant du module", example = "1") @PathVariable Long id) {
        moduleService.delete(clientId, id);
    }
}
