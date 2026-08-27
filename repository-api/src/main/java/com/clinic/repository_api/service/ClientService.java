package com.clinic.repository_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.repository_api.dto.ClientDto;
import com.clinic.repository_api.dto.ClientRequest;
import com.clinic.repository_api.model.Client;
import com.clinic.repository_api.model.enums.ClientStatut;
import com.clinic.repository_api.repository.ClientRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientDto> findAll() {
        return clientRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ClientDto findById(Long id) {
        return clientRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + id));
    }

    @Transactional
    public ClientDto create(ClientRequest request) {
        Client client = new Client();
        client.setNom(request.nom());
        client.setStatut(request.statut() != null ? request.statut() : ClientStatut.EN_REGLE);
        client.setNotes(request.notes());
        return toDto(clientRepository.save(client));
    }

    @Transactional
    public ClientDto update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + id));

        client.setNom(request.nom());
        if (request.statut() != null) client.setStatut(request.statut());
        client.setNotes(request.notes());

        return toDto(clientRepository.save(client));
    }

    private ClientDto toDto(Client c) {
        return new ClientDto(c.getId(), c.getNom(), c.getStatut(), c.getNotes());
    }
}