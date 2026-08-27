package com.clinic.repository_api.controller;

import com.clinic.repository_api.dto.ClientDto;
import com.clinic.repository_api.dto.ClientRequest;
import com.clinic.repository_api.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@PreAuthorize("hasRole('ADMIN')") // single role for now — applies to the whole controller
@Tag(name = "Clients", description = "Fiches clients — racine de toutes les autres ressources")
// Declared once here rather than on every method: every operation below is behind
// the same ADMIN check and the same JWT, so the 401/403 pair is identical throughout.
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Rôle ADMIN requis", content = @Content())
})
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Lister les clients",
            description = "Renvoie tous les clients. Pas de pagination : le référentiel compte quelques dizaines de fiches.")
    @GetMapping
    public List<ClientDto> findAll() {
        return clientService.findAll();
    }

    @Operation(summary = "Récupérer un client")
    @ApiResponse(responseCode = "404", description = "Client introuvable", content = @Content())
    @GetMapping("/{id}")
    public ClientDto findById(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long id) {
        return clientService.findById(id);
    }

    @Operation(summary = "Créer un client",
            description = "`statut` est optionnel et vaut `en regle` par défaut. L'identifiant est attribué par le serveur.")
    @ApiResponse(responseCode = "201", description = "Client créé")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto create(@Valid @RequestBody ClientRequest request) {
        return clientService.create(request);
    }

    @Operation(summary = "Mettre à jour un client",
            description = "Remplacement complet : les champs absents du corps sont écrasés, pas conservés.")
    @ApiResponse(responseCode = "400", description = "Validation échouée", content = @Content())
    @ApiResponse(responseCode = "404", description = "Client introuvable", content = @Content())
    @PutMapping("/{id}")
    public ClientDto update(
            @Parameter(description = "Identifiant du client", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {
        return clientService.update(id, request);
    }
}
