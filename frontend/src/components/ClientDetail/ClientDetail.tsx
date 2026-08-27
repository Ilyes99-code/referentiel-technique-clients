 import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useAuth } from "../../context/auth-context";
import {
  ApiError,
  createModule,
  createTechnicalAccess,
  getClient,
  getModules,
  getTechnicalAccessRecords,
  updateClient,
  updateModule,
  updateTechnicalAccess,
} from "../../services/api";
import type {
  ClientDto,
  ModuleDto,
  ModuleRequest,
  TechnicalAccessDto,
  TechnicalAccessRequest,
} from "../../types/models";
import { StatusBadge } from "../shared/StatusBadge";
import { ErrorBanner } from "../shared/ErrorBanner";
import { Toast } from "../shared/Toast";
import { ModuleList } from "./ModuleList";
import { ModuleFormModal } from "./ModuleFormModal";
import { AccessVault } from "./AccessVault";
import { FIXED_ACCESS_TYPES } from "./access-types";

const toAccessRequest = (record: TechnicalAccessDto): TechnicalAccessRequest => ({
  type: record.type,
  description: record.description ?? "",
  address: record.address ?? "",
  port: record.port,
  username: record.username ?? "",
  password: record.password ?? "",
  notes: record.notes ?? "",
});

const emptyAccessRequest = (type: string): TechnicalAccessRequest => ({
  type,
  description: "",
  address: "",
  port: null,
  username: "",
  password: "",
  notes: "",
});

export const ClientDetail: React.FC = () => {
  const { clientId: clientIdParam } = useParams<{ clientId: string }>();
  const clientId = Number(clientIdParam);
  const { token } = useAuth();

  const [client, setClient] = useState<ClientDto | null>(null);
  const [modules, setModules] = useState<ModuleDto[]>([]);
  const [accessRecords, setAccessRecords] = useState<TechnicalAccessDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [moduleModalOpen, setModuleModalOpen] = useState(false);
  const [editingModule, setEditingModule] = useState<ModuleDto | null>(null);
  const [savingModule, setSavingModule] = useState(false);
  const [savingAccess, setSavingAccess] = useState(false);

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const notify = (message: string) => {
    setToastMessage(message);
    window.setTimeout(() => setToastMessage(null), 2500);
  };

  const errorMessage = (err: unknown, fallback: string) =>
    err instanceof ApiError ? err.message : fallback;

  useEffect(() => {
    if (!token || Number.isNaN(clientId)) return;
    let cancelled = false;

    Promise.all([
      getClient(clientId, token),
      getModules(clientId, token),
      getTechnicalAccessRecords(clientId, token),
    ])
      .then(async ([clientData, moduleData, accessData]) => {
        if (cancelled) return;

        // Every client must always have these four access records — create
        // whichever ones are missing (e.g. a client that predates this feature).
        const missingTypes = FIXED_ACCESS_TYPES.filter(
          (type) => !accessData.some((record) => record.type === type),
        );
        const created = await Promise.all(
          missingTypes.map((type) => createTechnicalAccess(clientId, emptyAccessRequest(type), token)),
        );

        if (cancelled) return;
        setClient(clientData);
        setModules(moduleData);
        setAccessRecords([...accessData, ...created]);
        setError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(errorMessage(err, "Impossible de charger la fiche client"));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [clientId, token]);

  const openAddModule = () => {
    setEditingModule(null);
    setModuleModalOpen(true);
  };

  const openEditModule = (moduleItem: ModuleDto) => {
    setEditingModule(moduleItem);
    setModuleModalOpen(true);
  };

  const closeModuleModal = () => {
    setModuleModalOpen(false);
    setEditingModule(null);
  };

  const handleModuleSubmit = async (payload: ModuleRequest) => {
    if (!token) return;
    setSavingModule(true);
    try {
      if (editingModule) {
        const updated = await updateModule(clientId, editingModule.id, payload, token);
        setModules((current) => current.map((m) => (m.id === updated.id ? updated : m)));
        notify("Module mis à jour");
      } else {
        const created = await createModule(clientId, payload, token);
        setModules((current) => [created, ...current]);
        notify("Module enregistré");
      }
      closeModuleModal();
    } catch (err) {
      notify(errorMessage(err, "Échec de l'enregistrement du module"));
    } finally {
      setSavingModule(false);
    }
  };

  // Returns whether the save actually succeeded — AccessVault relies on this to
  // decide whether to leave edit mode. Resolving without throwing on failure
  // used to mean a failed save looked identical to a successful one and the
  // user's edits were silently discarded.
  const handleAccessSave = async (
    updatedRecords: TechnicalAccessDto[],
    notes: string,
  ): Promise<boolean> => {
    if (!token || !client) return false;
    setSavingAccess(true);
    try {
      const [savedRecords, updatedClient] = await Promise.all([
        Promise.all(
          updatedRecords.map((record) =>
            updateTechnicalAccess(clientId, record.id, toAccessRequest(record), token),
          ),
        ),
        updateClient(clientId, { nom: client.nom, statut: client.statut, notes }, token),
      ]);
      setAccessRecords(savedRecords);
      setClient(updatedClient);
      notify("Accès techniques mis à jour");
      return true;
    } catch (err) {
      notify(errorMessage(err, "Échec de la mise à jour des accès techniques — vos modifications n'ont pas été enregistrées"));
      return false;
    } finally {
      setSavingAccess(false);
    }
  };

  if (Number.isNaN(clientId)) {
    return (
      <div className="container-main">
        <Link to="/" className="back-link">
          ← Retour au tableau de bord
        </Link>
        <p className="mt-3">Client introuvable.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="container-main">
        <Link to="/" className="back-link">
          ← Retour au tableau de bord
        </Link>
        <div className="loading-state">Chargement de la fiche client…</div>
      </div>
    );
  }

  if (error || !client) {
    return (
      <div className="container-main">
        <Link to="/" className="back-link">
          ← Retour au tableau de bord
        </Link>
        <ErrorBanner message={error ?? "Client introuvable."} />
      </div>
    );
  }

  return (
    <div className="container-main">
      <Link to="/" className="back-link">
        ← Retour au tableau de bord
      </Link>

      <div className="page-header">
        <div>
          <h1>{client.nom}</h1>
          <p>
            <StatusBadge statut={client.statut} />
          </p>
        </div>
      </div>

      <div className="card">
        <div className="card-header-custom">
          <h2>📦 Modules installés</h2>
          <button type="button" className="btn btn-sm btn-primary add-btn" onClick={openAddModule}>
            + Ajouter un module
          </button>
        </div>
        <div className="card-body-custom">
          {modules.length > 0 ? (
            <ModuleList modules={modules} onEditModule={openEditModule} />
          ) : (
            <p className="text-muted small mb-0">Aucun module enregistré pour ce client.</p>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header-custom">
          <h2>🔐 Accès Techniques</h2>
        </div>
        <div className="card-body-custom">
          <AccessVault
            clientId={clientId}
            token={token}
            records={accessRecords}
            notes={client.notes ?? ""}
            submitting={savingAccess}
            onSave={handleAccessSave}
            onNotify={notify}
          />
        </div>
      </div>

      {moduleModalOpen && (
        <ModuleFormModal
          editingModule={editingModule}
          submitting={savingModule}
          onClose={closeModuleModal}
          onSubmit={handleModuleSubmit}
        />
      )}

      <Toast show={!!toastMessage} message={toastMessage ?? ""} />
    </div>
  );
};
