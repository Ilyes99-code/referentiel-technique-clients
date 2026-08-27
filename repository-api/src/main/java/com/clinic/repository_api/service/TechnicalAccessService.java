package com.clinic.repository_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.repository_api.dto.TechnicalAccessDto;
import com.clinic.repository_api.dto.TechnicalAccessRequest;
import com.clinic.repository_api.model.Client;
import com.clinic.repository_api.model.TechnicalAccess;
import com.clinic.repository_api.repository.ClientRepository;
import com.clinic.repository_api.repository.TechnicalAccessRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TechnicalAccessService {

    private final TechnicalAccessRepository accessRepository;
    private final ClientRepository clientRepository;

    public TechnicalAccessService(TechnicalAccessRepository accessRepository, ClientRepository clientRepository) {
        this.accessRepository = accessRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<TechnicalAccessDto> findAllByClient(Long clientId) {
        return accessRepository.findByClientIdOrderByIdAsc(clientId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TechnicalAccessDto findById(Long clientId, Long id) {
        return accessRepository.findByIdAndClientId(id, clientId)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Accès technique introuvable: " + id));
    }

    @Transactional
    public TechnicalAccessDto create(Long clientId, TechnicalAccessRequest request) {
        Client client = requireClient(clientId);

        TechnicalAccess access = new TechnicalAccess();
        access.setClient(client);
        apply(access, request);

        return toDto(accessRepository.save(access));
    }

    @Transactional
    public TechnicalAccessDto update(Long clientId, Long id, TechnicalAccessRequest request) {
        TechnicalAccess access = accessRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Accès technique introuvable: " + id));

        apply(access, request);
        return toDto(accessRepository.save(access));
    }

    @Transactional
    public void delete(Long clientId, Long id) {
        TechnicalAccess access = accessRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Accès technique introuvable: " + id));
        accessRepository.delete(access);
    }

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));
    }

    private void apply(TechnicalAccess access, TechnicalAccessRequest request) {
        access.setType(request.type());
        access.setDescription(request.description());
        access.setAddress(request.address());
        access.setPort(request.port());
        access.setUsername(request.username());
        access.setPassword(request.password());
        access.setNotes(request.notes());
    }

    private TechnicalAccessDto toDto(TechnicalAccess access) {
        return new TechnicalAccessDto(
                access.getId(),
                access.getClient() != null ? access.getClient().getId() : null,
                access.getType(),
                access.getDescription(),
                access.getAddress(),
                access.getPort(),
                access.getUsername(),
                access.getPassword(),
                access.getNotes()
        );
    }
}