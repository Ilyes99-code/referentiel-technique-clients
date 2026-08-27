package com.clinic.repository_api.repository;

import com.clinic.repository_api.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}