import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { useAuth } from "../../context/auth-context";
import { getClients, ApiError } from "../../services/api";
import type { ClientDto } from "../../types/models";
import { ClientTable } from "./ClientTable";
import { ErrorBanner } from "../shared/ErrorBanner";

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
    </div>
  );
};
