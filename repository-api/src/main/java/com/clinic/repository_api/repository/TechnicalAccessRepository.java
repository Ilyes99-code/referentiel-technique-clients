package com.clinic.repository_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.repository_api.model.TechnicalAccess;

public interface TechnicalAccessRepository extends JpaRepository<TechnicalAccess, Long> {
    List<TechnicalAccess> findByClientIdOrderByIdAsc(Long clientId);
    Optional<TechnicalAccess> findByIdAndClientId(Long id, Long clientId);
}