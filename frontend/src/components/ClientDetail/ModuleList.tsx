import React from "react";
import type { ModuleDto } from "../../types/models";

interface ModuleListProps {
  modules: ModuleDto[];
  onEditModule: (moduleItem: ModuleDto) => void;
}

const NOT_SET = "Non défini";

export const ModuleList: React.FC<ModuleListProps> = ({ modules, onEditModule }) => (
  <ul className="module-list">
    {modules.map((moduleItem) => (
      <li className="module-item" key={moduleItem.id}>
        <div className="mod-top-row">
          <div className="mod-title">
            {moduleItem.module}
            <span className="badge-version">v{moduleItem.version ?? NOT_SET}</span>
          </div>
          <button
            type="button"
            className="module-edit-btn"
            onClick={() => onEditModule(moduleItem)}
          >
            Modifier
          </button>
        </div>

        <div className="mod-meta">
          <span>
            <span className="label">Dernière MAJ</span> {moduleItem.dateMaj ?? NOT_SET}
          </span>
          <span>
            <span className="label">Date MEP</span> {moduleItem.dateMep ?? NOT_SET}
          </span>
        </div>

        <div className="mod-links">
          <div className="link-row link-externe">
            <span className="link-label">🌐 Externe</span>
            {moduleItem.lienExterne ? (
              <a href={moduleItem.lienExterne} target="_blank" rel="noreferrer">
                {moduleItem.lienExterne}
              </a>
            ) : (
              <span className="text-muted">{NOT_SET}</span>
            )}
          </div>
          <div className="link-row link-interne">
            <span className="link-label">🏠 Interne</span>
            {moduleItem.lienInterne ? (
              <a href={moduleItem.lienInterne} target="_blank" rel="noreferrer">
                {moduleItem.lienInterne}
              </a>
            ) : (
              <span className="text-muted">{NOT_SET}</span>
            )}
          </div>
        </div>
      </li>
    ))}
  </ul>
);
