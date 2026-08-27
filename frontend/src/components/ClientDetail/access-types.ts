export type AccessFieldKey = "username" | "address" | "description" | "port" | "password";

export interface AccessFieldSpec {
  key: AccessFieldKey;
  label: string;
}

export interface AccessTypeConfig {
  icon: string;
  sub: string;
  fields: AccessFieldSpec[];
}

// Fixed by design — every client has exactly these four access records, matching
// the fields the backend's generic TechnicalAccessDto actually has (type,
// description, address, port, username, password). ClientDetail guarantees all
// four exist (auto-creating any missing ones) before rendering AccessVault.
export const ACCESS_TYPE_CONFIG: Record<string, AccessTypeConfig> = {
  LogMeIn: {
    icon: "🖥️",
    sub: "Accès bureau à distance",
    fields: [
      { key: "username", label: "Utilisateur" },
      { key: "address", label: "Serveur" },
      { key: "description", label: "Nom" },
      { key: "password", label: "Mot de passe" },
    ],
  },
  "SQL Server": {
    icon: "🗄️",
    sub: "Base de données principale",
    fields: [
      { key: "address", label: "Instance" },
      { key: "port", label: "Port" },
      { key: "username", label: "Utilisateur" },
      { key: "password", label: "Mot de passe" },
    ],
  },
  "Admin Access": {
    icon: "🔑",
    sub: "Compte administrateur",
    fields: [{ key: "password", label: "Mot de passe" }],
  },
  "VPN Accès": {
    icon: "🔒",
    sub: "Réseau privé virtuel",
    fields: [
      { key: "address", label: "Adresse" },
      { key: "username", label: "Utilisateur" },
      { key: "password", label: "Mot de passe" },
    ],
  },
};

export const FIXED_ACCESS_TYPES = Object.keys(ACCESS_TYPE_CONFIG);
