import React, { useState } from "react";
import type { ModuleDto, ModuleRequest } from "../../types/models";

const emptyForm: ModuleRequest = {
  module: "",
  version: "",
  dateMaj: "",
  dateMep: "",
  lienExterne: "",
  lienInterne: "",
};

const toFormValues = (moduleItem: ModuleDto | null): ModuleRequest =>
  moduleItem
    ? {
        module: moduleItem.module,
        version: moduleItem.version ?? "",
        dateMaj: moduleItem.dateMaj ?? "",
        dateMep: moduleItem.dateMep ?? "",
        lienExterne: moduleItem.lienExterne ?? "",
        lienInterne: moduleItem.lienInterne ?? "",
      }
    : emptyForm;

interface ModuleFormModalProps {
  editingModule: ModuleDto | null;
  submitting: boolean;
  onClose: () => void;
  onSubmit: (payload: ModuleRequest) => void;
}

export const ModuleFormModal: React.FC<ModuleFormModalProps> = ({
  editingModule,
  submitting,
  onClose,
  onSubmit,
}) => {
  // editingModule is fixed for this component's lifetime — the parent always
  // unmounts and remounts the modal (see the `{moduleModalOpen && <ModuleFormModal .../>}`
  // guard in ClientDetail) rather than swapping the target while it stays open, so a
  // lazy initializer is enough; no effect needed to resync on prop change.
  const [form, setForm] = useState<ModuleRequest>(() => toFormValues(editingModule));

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.module.trim()) return;
    onSubmit({ ...form, module: form.module.trim() });
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h3>{editingModule ? "Modifier le module" : "Nouveau module"}</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fermer">
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="module-form">
          <div className="form-grid">
            <label>
              <span>Nom du module</span>
              <input
                type="text"
                name="module"
                value={form.module}
                onChange={handleChange}
                placeholder="Ex. DMI Web"
                required
              />
            </label>

            <label>
              <span>Version</span>
              <input
                type="text"
                name="version"
                value={form.version}
                onChange={handleChange}
                placeholder="3.4.0"
              />
            </label>

            <label>
              <span>Dernière MAJ</span>
              <input type="date" name="dateMaj" value={form.dateMaj} onChange={handleChange} />
            </label>

            <label>
              <span>Date MEP</span>
              <input type="date" name="dateMep" value={form.dateMep} onChange={handleChange} />
            </label>

            <label className="full-width">
              <span>Lien externe</span>
              <input
                type="url"
                name="lienExterne"
                value={form.lienExterne}
                onChange={handleChange}
                placeholder="https://..."
              />
            </label>

            <label className="full-width">
              <span>Lien interne</span>
              <input
                type="url"
                name="lienInterne"
                value={form.lienInterne}
                onChange={handleChange}
                placeholder="http://..."
              />
            </label>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Annuler
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting
                ? "Enregistrement..."
                : editingModule
                  ? "Mettre à jour le module"
                  : "Enregistrer le module"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
