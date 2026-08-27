package com.clinic.repository_api.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.clinic.repository_api.model.Client;
import com.clinic.repository_api.model.ClientModule;
import com.clinic.repository_api.model.TechnicalAccess;
import com.clinic.repository_api.model.enums.ClientStatut;
import com.clinic.repository_api.repository.ClientModuleRepository;
import com.clinic.repository_api.repository.ClientRepository;
import com.clinic.repository_api.repository.TechnicalAccessRepository;

/**
 * Seeds a handful of demo clients so the app isn't empty on first run locally.
 * Dev-profile only — real deployments should never get fake clinic data.
 *
 * Deliberately varied: different statuses, some clients fully populated, some
 * with no modules/access records at all, some with partial/null optional
 * fields — this is what actually exercises the frontend's defensive rendering
 * (ModuleList's "Non défini" fallback, AccessVault's empty-state message and
 * its per-field filtering of unset values) instead of every card looking
 * identical.
 */
@Component
@Profile("dev")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ClientRepository clientRepository;
    private final ClientModuleRepository moduleRepository;
    private final TechnicalAccessRepository accessRepository;

    public DemoDataInitializer(
            ClientRepository clientRepository,
            ClientModuleRepository moduleRepository,
            TechnicalAccessRepository accessRepository) {
        this.clientRepository = clientRepository;
        this.moduleRepository = moduleRepository;
        this.accessRepository = accessRepository;
    }

    @Override
    public void run(String... args) {
        if (clientRepository.count() > 0) {
            return;
        }

        seedClinicIbnSina();
        seedPolycliniqueSfax();
        seedCentreDiagnosticSousse();
        seedCabinetTrabelsi();
        seedHopitalKairouan();
        seedCliniqueElManar();

        log.info("Seeded {} demo clients", clientRepository.count());
    }

    private void seedClinicIbnSina() {
        Client client = saveClient("Clinique Ibn Sina", ClientStatut.EN_REGLE);

        saveModule(client, "DMI Web", "3.4.0",
                LocalDate.of(2026, 6, 10), LocalDate.of(2023, 1, 15),
                "https://dmi.clinique-ibnsina.tn", "http://192.168.1.10/dmi");
        saveModule(client, "Compte Rendu Web", "2.1.0",
                LocalDate.of(2026, 2, 3), LocalDate.of(2022, 11, 2),
                "https://cr.clinique-ibnsina.tn", "http://192.168.1.11/cr");

        // Type strings must match the frontend's fixed access-record types exactly
        // (LogMeIn / SQL Server / Admin Access / VPN Accès) — anything else is
        // invisible in AccessVault and gets silently shadowed by an auto-created
        // blank record of the expected type.
        saveAccess(client, "VPN Accès", null, "vpn.clinique-ibnsina.tn", null,
                "admin", "Vpn#2026!", null);
    }

    private void seedPolycliniqueSfax() {
        Client client = saveClient("Polyclinique Sfax", ClientStatut.EN_REGLE);

        // No dateMaj and no links set — exercises ModuleList's null fallbacks.
        saveModule(client, "DMI Web", "3.2.1", null, LocalDate.of(2024, 5, 20), null, null);

        // No access records at all — exercises AccessVault's empty state.
    }

    private void seedCentreDiagnosticSousse() {
        // Suspended, no modules, no access records — the fully-empty case.
        saveClient("Centre Diagnostic Sousse", ClientStatut.SUSPENDU);
    }

    private void seedCabinetTrabelsi() {
        Client client = saveClient("Cabinet Dr. Trabelsi", ClientStatut.EN_REGLE);

        // Only the required module name is set — every other field null.
        saveModule(client, "Compte Rendu Web", null, null, null, "https://cr.cabinet-trabelsi.tn", null);

        // Only the password field set — description/address/port/username all null,
        // so AccessVault should only render the one field that has a value.
        saveAccess(client, "Admin Access", null, null, null, null, "Adm!n2026#", null);
    }

    private void seedHopitalKairouan() {
        Client client = saveClient("Hôpital Régional Kairouan", ClientStatut.SUSPENDU);

        saveAccess(client, "SQL Server", null, "10.20.0.5", 1433,
                "sa", "SqlP@ss2026", null);
        // No password on this one — access without a password should still render cleanly.
        saveAccess(client, "LogMeIn", "Bureau à distance secours", "10.20.0.6", null,
                "support", null, "Utilisé uniquement en cas d'incident.");
    }

    private void seedCliniqueElManar() {
        Client client = saveClient("Clinique El Manar", ClientStatut.EN_REGLE);

        saveModule(client, "DMI Web", "3.4.0",
                LocalDate.of(2026, 7, 1), LocalDate.of(2024, 3, 12),
                "https://dmi.clinique-elmanar.tn", "http://10.0.0.20/dmi");
        // A second module with no version/dates/links at all.
        saveModule(client, "Portail Patient", null, null, null, null, null);

        // Only address + notes set — no username/password.
        saveAccess(client, "VPN Accès", null, "vpn.clinique-elmanar.tn", null, null, null,
                "Accès partagé avec le prestataire réseau externe.");
    }

    private Client saveClient(String nom, ClientStatut statut) {
        Client client = new Client();
        client.setNom(nom);
        client.setStatut(statut);
        return clientRepository.save(client);
    }

    private void saveModule(Client client, String module, String version, LocalDate dateMaj,
            LocalDate dateMep, String lienExterne, String lienInterne) {
        ClientModule m = new ClientModule();
        m.setClient(client);
        m.setModule(module);
        m.setVersion(version);
        m.setDateMaj(dateMaj);
        m.setDateMep(dateMep);
        m.setLienExterne(lienExterne);
        m.setLienInterne(lienInterne);
        moduleRepository.save(m);
    }

    private void saveAccess(Client client, String type, String description, String address,
            Integer port, String username, String password, String notes) {
        TechnicalAccess access = new TechnicalAccess();
        access.setClient(client);
        access.setType(type);
        access.setDescription(description);
        access.setAddress(address);
        access.setPort(port);
        access.setUsername(username);
        access.setPassword(password);
        access.setNotes(notes);
        accessRepository.save(access);
    }
}
