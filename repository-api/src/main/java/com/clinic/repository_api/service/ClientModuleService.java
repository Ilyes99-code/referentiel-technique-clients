package com.clinic.repository_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.repository_api.dto.ModuleDto;
import com.clinic.repository_api.dto.ModuleRequest;
import com.clinic.repository_api.model.Client;
import com.clinic.repository_api.model.ClientModule;
import com.clinic.repository_api.repository.ClientModuleRepository;
import com.clinic.repository_api.repository.ClientRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ClientModuleService {

    private final ClientModuleRepository moduleRepository;
    private final ClientRepository clientRepository;

    public ClientModuleService(ClientModuleRepository moduleRepository, ClientRepository clientRepository) {
        this.moduleRepository = moduleRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ModuleDto> findAllByClient(Long clientId) {
        return moduleRepository.findByClientIdOrderByIdAsc(clientId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ModuleDto findById(Long clientId, Long id) {
        return moduleRepository.findByIdAndClientId(id, clientId)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Module introuvable: " + id));
    }

    @Transactional
    public ModuleDto create(Long clientId, ModuleRequest request) {
        Client client = requireClient(clientId);

        ClientModule module = new ClientModule();
        module.setClient(client);
        apply(module, request);

        return toDto(moduleRepository.save(module));
    }

    @Transactional
    public ModuleDto update(Long clientId, Long id, ModuleRequest request) {
        ClientModule module = moduleRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Module introuvable: " + id));

        apply(module, request);
        return toDto(moduleRepository.save(module));
    }

    @Transactional
    public void delete(Long clientId, Long id) {
        ClientModule module = moduleRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Module introuvable: " + id));
        moduleRepository.delete(module);
    }

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));
    }

    private void apply(ClientModule module, ModuleRequest request) {
        module.setModule(request.module());
        module.setVersion(request.version());
        module.setDateMaj(request.dateMaj());
        module.setDateMep(request.dateMep());
        module.setLienExterne(request.lienExterne());
        module.setLienInterne(request.lienInterne());
    }

    private ModuleDto toDto(ClientModule module) {
        return new ModuleDto(
                module.getId(),
                module.getClient() != null ? module.getClient().getId() : null,
                module.getModule(),
                module.getVersion(),
                module.getDateMaj(),
                module.getDateMep(),
                module.getLienExterne(),
                module.getLienInterne()
        );
    }
}