package com.clinic.repository_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.repository_api.model.ClientModule;

public interface ClientModuleRepository extends JpaRepository<ClientModule, Long> {
    List<ClientModule> findByClientIdOrderByIdAsc(Long clientId);
    Optional<ClientModule> findByIdAndClientId(Long id, Long clientId);
}