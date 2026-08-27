import React from "react";
import { StatusBadge } from "../shared/StatusBadge";
import type { ClientDto } from "../../types/models";

type SortKey = "nom" | "statut";

interface ClientTableProps {
  rows: ClientDto[];
  sortKey: SortKey;
  sortAsc: boolean;
  onSort: (key: SortKey) => void;
  onRowClick: (row: ClientDto) => void;
}

// Pure presentational component: all state (search, sort) lives in Dashboard.
export const ClientTable: React.FC<ClientTableProps> = ({ rows, sortKey, sortAsc, onSort, onRowClick }) => {
  const arrowFor = (key: SortKey) => {
    if (key !== sortKey) return "▲"; // dim, shown via CSS opacity when not sorted
    return sortAsc ? "▲" : "▼";
  };

  return (
    <div className="card p-0 overflow-hidden">
      <table className="table table-borderless mb-0">
        <thead>
          <tr>
            <th
              className={`sortable${sortKey === "nom" ? " sorted" : ""}`}
              onClick={() => onSort("nom")}
            >
              Client <span className="arrow">{arrowFor("nom")}</span>
            </th>
            <th>Statut</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} onClick={() => onRowClick(row)}>
              <td className="client-name">{row.nom}</td>
              <td>
                <StatusBadge statut={row.statut} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {rows.length === 0 && (
        <div className="empty-state" style={{ display: "block" }}>
          Aucun client trouvé pour cette recherche.
        </div>
      )}
    </div>
  );
};
