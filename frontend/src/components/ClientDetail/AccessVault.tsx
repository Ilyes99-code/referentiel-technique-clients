import React, { useState } from "react";
import type { TechnicalAccessDto } from "../../types/models";
import { ACCESS_TYPE_CONFIG, FIXED_ACCESS_TYPES, type AccessFieldKey } from "./access-types";
import { AccessImageGallery } from "./AccessImageGallery";

type FieldKey = AccessFieldKey;

interface DraftFields {
  username: string;
  address: string;
  description: string;
  port: string;
  password: string;
}

const emptyDraft: DraftFields = { username: "", address: "", description: "", port: "", password: "" };

const toDraft = (record: TechnicalAccessDto | undefined): DraftFields =>
  record
    ? {
        username: record.username ?? "",
        address: record.address ?? "",
        description: record.description ?? "",
        port: record.port != null ? String(record.port) : "",
        password: record.password ?? "",
      }
    : emptyDraft;

const displayValue = (record: TechnicalAccessDto | undefined, key: FieldKey): string => {
  if (!record) return "";
  switch (key) {
    case "username":
      return record.username ?? "";
    case "address":
      return record.address ?? "";
    case "description":
      return record.description ?? "";
    case "password":
      return record.password ?? "";
    case "port":
      return record.port != null ? String(record.port) : "";
  }
};

interface AccessVaultProps {
  clientId: number;
  token: string | null;
  records: TechnicalAccessDto[];
  notes: string;
  submitting: boolean;
  // Returns whether the save actually succeeded — the caller must NOT swallow
  // errors and resolve true, or a failed save looks identical to a successful
  // one and the user's edits get silently discarded (see finishEditing below).
  onSave: (records: TechnicalAccessDto[], notes: string) => Promise<boolean>;
  onNotify: (message: string) => void;
}

const PORT_MIN = 1;
const PORT_MAX = 65535;

const isValidPortDraft = (value: string): boolean => {
  const trimmed = value.trim();
  if (trimmed === "") return true;
  if (!/^\d+$/.test(trimmed)) return false;
  const parsed = Number(trimmed);
  return parsed >= PORT_MIN && parsed <= PORT_MAX;
};

export const AccessVault: React.FC<AccessVaultProps> = ({
  clientId,
  token,
  records,
  notes,
  submitting,
  onSave,
  onNotify,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [draftByType, setDraftByType] = useState<Record<string, DraftFields>>({});
  const [draftNotes, setDraftNotes] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const [copiedMessage, setCopiedMessage] = useState<{ x: number; y: number } | null>(null);
  const copyTimeoutRef = React.useRef<number | undefined>(undefined);

  const recordByType = (type: string) => records.find((r) => r.type === type);

  const startEditing = () => {
    const nextDraft: Record<string, DraftFields> = {};
    for (const type of FIXED_ACCESS_TYPES) {
      nextDraft[type] = toDraft(recordByType(type));
    }
    setDraftByType(nextDraft);
    setDraftNotes(notes);
    setValidationError(null);
    setIsEditing(true);
  };

  const updateDraftField = (type: string, key: FieldKey, value: string) => {
    setDraftByType((current) => ({
      ...current,
      [type]: { ...current[type], [key]: value },
    }));
  };

  const finishEditing = async () => {
    const invalidType = FIXED_ACCESS_TYPES.find(
      (type) => !isValidPortDraft(draftByType[type]?.port ?? ""),
    );
    if (invalidType) {
      setValidationError(`Port invalide pour "${invalidType}" — doit être un nombre entre ${PORT_MIN} et ${PORT_MAX}.`);
      return;
    }

    const updated = FIXED_ACCESS_TYPES
      .map((type): TechnicalAccessDto | null => {
        const original = recordByType(type);
        if (!original) return null; // shouldn't happen — ClientDetail guarantees all 4 exist
        const draft = draftByType[type] ?? emptyDraft;
        return {
          ...original,
          username: draft.username,
          address: draft.address,
          description: draft.description,
          port: draft.port.trim() === "" ? null : Number(draft.port),
          password: draft.password,
        };
      })
      .filter((record): record is TechnicalAccessDto => record !== null);

    setValidationError(null);
    const succeeded = await onSave(updated, draftNotes);
    // Only leave edit mode on success — otherwise the draft (and whatever the
    // user just retyped) would be silently thrown away on the next render.
    if (succeeded) {
      setIsEditing(false);
    }
  };

  const copyToClipboard = async (
    value: string,
    event: React.MouseEvent | React.KeyboardEvent,
  ) => {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      const position =
        "clientX" in event
          ? { x: event.clientX, y: event.clientY }
          : { x: window.innerWidth / 2, y: window.innerHeight / 2 };
      setCopiedMessage(position);
      window.clearTimeout(copyTimeoutRef.current);
      copyTimeoutRef.current = window.setTimeout(() => setCopiedMessage(null), 900);
    } catch (error) {
      console.error("Clipboard copy failed:", error);
    }
  };

  return (
    <div className="access-vault">
      <div className="access-vault-toolbar">
        <button
          type="button"
          className="btn btn-sm btn-secondary access-edit-btn"
          onClick={() => (isEditing ? finishEditing() : startEditing())}
          disabled={submitting}
        >
          {isEditing ? (submitting ? "Enregistrement..." : "Terminer") : "Modifier"}
        </button>
      </div>

      {validationError && (
        <div className="login-error error-banner" role="alert">
          <span className="login-error-mark">!</span>
          <span>{validationError}</span>
        </div>
      )}

      {FIXED_ACCESS_TYPES.map((type) => {
        const config = ACCESS_TYPE_CONFIG[type];
        const record = recordByType(type);
        const draft = draftByType[type];

        return (
          <div className="access-row" key={type}>
            <div className="access-label">
              <div className="access-icon">{config.icon}</div>
              <div>
                <div className="access-name">{type}</div>
                <div className="access-sub">{config.sub}</div>
              </div>
            </div>

            <div className="detailed-fields">
              {config.fields.map((field) => {
                if (isEditing && draft) {
                  return (
                    <label className="field-row field-row-edit" key={field.key}>
                      <span className="field-label">{field.label}</span>
                      <input
                        type={field.key === "port" ? "number" : "text"}
                        min={field.key === "port" ? PORT_MIN : undefined}
                        max={field.key === "port" ? PORT_MAX : undefined}
                        className="field-input"
                        value={draft[field.key]}
                        onChange={(event) => updateDraftField(type, field.key, event.target.value)}
                      />
                    </label>
                  );
                }

                const value = displayValue(record, field.key);
                return (
                  <div
                    className="field-row field-row-clickable"
                    key={field.key}
                    onClick={(event) => copyToClipboard(value, event)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        copyToClipboard(value, event);
                      }
                    }}
                    title={`Copier ${field.label}`}
                  >
                    <span className="field-label">{field.label}</span>
                    <span className="field-value field-value-plain">{value || "—"}</span>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}

      {/* Sits directly under the last access row ("VPN Accès"). Self-contained:
          it owns its own fetch/upload/delete lifecycle rather than participating
          in the vault's Modifier/Terminer draft cycle, because an upload is
          committed the moment it succeeds — there is nothing to stage. */}
      {token && (
        <AccessImageGallery clientId={clientId} token={token} onNotify={onNotify} />
      )}

      <div className="access-note-section">
        <label className="access-note-label">Note</label>
        {isEditing ? (
          <textarea
            className="access-note-textarea"
            value={draftNotes}
            onChange={(event) => setDraftNotes(event.target.value)}
          />
        ) : (
          <div className="access-note-box">{notes || "Aucune note pour ce client."}</div>
        )}
      </div>

      {copiedMessage && (
        <div className="copy-toast" style={{ left: copiedMessage.x, top: copiedMessage.y }}>
          Copié !
        </div>
      )}
    </div>
  );
};
