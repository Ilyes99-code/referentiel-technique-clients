package com.clinic.repository_api.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
public class ClientModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "app_name", nullable = false, length = 255)
    private String module;

    @Column(length = 100)
    private String version;

    @Column(name = "date_maj")
    private LocalDate dateMaj;

    @Column(name = "date_mep")
    private LocalDate dateMep;

    @Column(name = "lien_externe", length = 500)
    private String lienExterne;

    @Column(name = "lien_interne", length = 500)
    private String lienInterne;

    public ClientModule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(LocalDate dateMaj) {
        this.dateMaj = dateMaj;
    }

    public LocalDate getDateMep() {
        return dateMep;
    }

    public void setDateMep(LocalDate dateMep) {
        this.dateMep = dateMep;
    }

    public String getLienExterne() {
        return lienExterne;
    }

    public void setLienExterne(String lienExterne) {
        this.lienExterne = lienExterne;
    }

    public String getLienInterne() {
        return lienInterne;
    }

    public void setLienInterne(String lienInterne) {
        this.lienInterne = lienInterne;
    }
}