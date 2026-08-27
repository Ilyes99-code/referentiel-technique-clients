package com.clinic.repository_api.model;

import com.clinic.repository_api.model.enums.ClientStatut;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClientStatut statut = ClientStatut.EN_REGLE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Client() {
    }

    public Client(Long id, String nom, ClientStatut statut) {
        this.id = id;
        this.nom = nom;
        this.statut = statut != null ? statut : ClientStatut.EN_REGLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public ClientStatut getStatut() {
        return statut;
    }

    public void setStatut(ClientStatut statut) {
        this.statut = statut != null ? statut : ClientStatut.EN_REGLE;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}