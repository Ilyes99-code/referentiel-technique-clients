import React, { useState } from "react";
import type { ClientRequest } from "../../types/models";

interface ClientFormModalProps {
  submitting: boolean;
  onClose: () => void;
  onSubmit: (payload: ClientRequest) => void;
}

/**
 * Create-a-client dialog. Deliberately the same shape as ModuleFormModal — same
 * .modal-backdrop / .modal-card / .form-grid markup, so it needs no CSS of its own.
 *
 * Only `nom` is asked for: ClientRequest treats `statut` as optional and the service
 * defaults it to EN_REGLE, and `notes` is free text better filled in on the client's
 * own page than in a creation dialog.
 */
export const ClientFormModal: React.FC<ClientFormModalProps> = ({
  submitting,
  onClose,
  onSubmit,
}) => {
  const [nom, setNom] = useState("");

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = nom.trim();
    if (!trimmed) return;
    onSubmit({ nom: trimmed });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h3>Nouveau client</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="module-form">
          <div className="form-grid">
            <label className="full-width">
              <span>Nom du client</span>
              <input
                type="text"
                name="nom"
                value={nom}
                onChange={(event) => setNom(event.target.value)}
                placeholder="Ex. Clinique Ibn Sina"
                maxLength={255}
                autoFocus
                required
              />
            </label>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Annuler
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting || !nom.trim()}>
              {submitting ? "Création..." : "Créer le client"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
