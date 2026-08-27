import React from "react";
import type { ClientStatut } from "../../types/models";

interface StatusBadgeProps {
  statut: ClientStatut;
}

const STATUS_LABELS: Record<ClientStatut, string> = {
  "en regle": "En règle",
  suspendu: "Suspendu",
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({ statut }) => {
  const isEnRegle = statut === "en regle";
  return (
    <span className={isEnRegle ? "badge badge-statut-en-regle" : "badge badge-statut-suspendu"}>
      {STATUS_LABELS[statut] ?? statut}
    </span>
  );
};
