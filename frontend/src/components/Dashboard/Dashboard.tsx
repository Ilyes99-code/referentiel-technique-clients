import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { LogOut, Plus } from "lucide-react";
import { useAuth } from "../../context/auth-context";
import { getClients, createClient, ApiError } from "../../services/api";
import type { ClientDto, ClientRequest } from "../../types/models";
import { ClientTable } from "./ClientTable";
import { ClientFormModal } from "./ClientFormModal";
import { ErrorBanner } from "../shared/ErrorBanner";
import { Toast } from "../shared/Toast";

type SortKey = "nom" | "statut";

export const Dashboard: React.FC = () => {
  const { token, logout } = useAuth();
  const navigate = useNavigate();

  const [clients, setClients] = useState<ClientDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("nom");
  const [sortAsc, setSortAsc] = useState(true);

  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const notify = (message: string) => {
    setToastMessage(message);
    window.setTimeout(() => setToastMessage(null), 2500);
  };

  useEffect(() => {
    if (!token) return;
    let cancelled = false;

    getClients(token)
      .then((data) => {
        if (cancelled) return;
        setClients(data);
        setError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : "Impossible de charger les clients");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  const visibleRows = useMemo(() => {
    const term = searchTerm.toLowerCase();
    const filtered = clients.filter((row) => row.nom.toLowerCase().includes(term));

    return [...filtered].sort((a, b) => {
      const valueA = a[sortKey];
      const valueB = b[sortKey];
      if (valueA < valueB) return sortAsc ? -1 : 1;
      if (valueA > valueB) return sortAsc ? 1 : -1;
      return 0;
    });
  }, [clients, searchTerm, sortKey, sortAsc]);

  const handleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortAsc((previous) => !previous);
    } else {
      setSortKey(key);
      setSortAsc(true);
    }
  };

  const handleRowClick = (row: ClientDto) => {
    navigate(`/clients/${row.id}`);
  };

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const handleCreateClient = async (payload: ClientRequest) => {
    if (!token) return;
    setCreating(true);
    try {
      const created = await createClient(payload, token);
      setClients((current) => [...current, created]);
      setCreateOpen(false);
      notify("Client créé");
      // Straight into the new client's page — creating one is almost always the
      // first step of filling it in.
      navigate(`/clients/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Impossible de créer le client");
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="container-main">
      <div className="page-header">
        <div>
          <h1>Référentiel Technique Clients</h1>
          <p>Vue simplifiée des clients et de leur statut</p>
        </div>
        <div className="dashboard-header-actions">
          <span className="badge bg-primary rounded-pill px-3 py-2">
            {clients.length} clients
          </span>
          <button
            type="button"
            className="btn btn-sm btn-primary add-btn"
            onClick={() => setCreateOpen(true)}
          >
            <Plus size={15} aria-hidden="true" />
            <span>Ajouter un client</span>
          </button>
          <button type="button" className="logout-button" onClick={handleLogout}>
            <LogOut size={16} aria-hidden="true" />
            <span>Déconnexion</span>
          </button>
        </div>
      </div>

      {error && <ErrorBanner message={error} />}

      <div className="card p-3 mb-3">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <div className="search-bar flex-grow-1">
            <input
              type="text"
              className="form-control"
              placeholder="🔍 Rechercher un client..."
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
          <div className="text-muted small">{visibleRows.length} résultat(s)</div>
        </div>
      </div>

      {loading ? (
        <div className="loading-state">Chargement des clients…</div>
      ) : clients.length === 0 ? (
        /* A brand-new deployment starts with no clients, and everything else in the
           app hangs off one — so an empty table here is a dead end rather than just
           an empty screen. */
        <div className="empty-state">
          <div className="empty-state-icon" aria-hidden="true">📋</div>
          <h2>Aucun client enregistré</h2>
          <p>
            Commencez par ajouter un client : vous pourrez ensuite y rattacher ses
            modules, ses accès techniques et ses captures.
          </p>
          <button
            type="button"
            className="btn btn-primary add-btn"
            onClick={() => setCreateOpen(true)}
          >
            <Plus size={16} aria-hidden="true" />
            <span>Ajouter un client</span>
          </button>
        </div>
      ) : (
        <>
          <ClientTable
            rows={visibleRows}
            sortKey={sortKey}
            sortAsc={sortAsc}
            onSort={handleSort}
            onRowClick={handleRowClick}
          />
          <p className="text-muted small mt-3">
            💡 Cliquez sur une ligne pour ouvrir la fiche détaillée du client.
          </p>
        </>
      )}

      {createOpen && (
        <ClientFormModal
          submitting={creating}
          onClose={() => setCreateOpen(false)}
          onSubmit={handleCreateClient}
        />
      )}

      <Toast show={!!toastMessage} message={toastMessage ?? ""} />
    </div>
  );
};
