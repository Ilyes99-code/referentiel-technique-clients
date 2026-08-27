import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, KeyRound, Loader2, Lock, ShieldCheck, UserRound } from "lucide-react";
import { login as apiLogin, ApiError } from "../services/api";
import { useAuth } from "../context/auth-context";

interface LocationState {
  from?: { pathname?: string };
}

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const from = (location.state as LocationState | null)?.from?.pathname ?? "/dashboard";

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const authResponse = await apiLogin(username, password);
      login(authResponse);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "La connexion a échoué");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-shell">
      <section className="login-frame" aria-labelledby="login-title">
        <div className="login-panel">
          <div className="login-brand">
            <span className="login-brand-mark" aria-hidden="true">
              <ShieldCheck size={23} />
            </span>
            <span className="login-brand-name">Référentiel Technique</span>
          </div>

          {/* The form previously had no heading at all — two unlabelled fields on a
              blank card, with nothing saying what was being signed into. */}
          <div className="login-heading">
            <h1 id="login-title">Connexion</h1>
            <p>Accédez au référentiel technique de vos clients.</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            <div className="login-field">
              <label htmlFor="login-username">Identifiant</label>
              <div className="login-input-wrap">
                <UserRound size={17} aria-hidden="true" />
                <input
                  id="login-username"
                  type="text"
                  required
                  // Saves a click on a screen whose only purpose is this one field.
                  autoFocus
                  autoComplete="username"
                  disabled={loading}
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="admin"
                />
              </div>
            </div>

            <div className="login-field">
              <label htmlFor="login-password">Mot de passe</label>
              <div className="login-input-wrap">
                <KeyRound size={17} aria-hidden="true" />
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  required
                  autoComplete="current-password"
                  disabled={loading}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword((visible) => !visible)}
                  // tabIndex -1 keeps Tab going straight from the password field to
                  // the submit button, which is what someone typing a password
                  // expects; the toggle stays reachable by click.
                  tabIndex={-1}
                  aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </div>

            {error && (
              <div className="login-error" role="alert">
                <span className="login-error-mark" aria-hidden="true">!</span>
                <span>{error}</span>
              </div>
            )}

            <button type="submit" disabled={loading} className="login-submit" aria-busy={loading}>
              {loading ? (
                <>
                  <Loader2 size={17} className="login-spinner" aria-hidden="true" />
                  <span>Connexion…</span>
                </>
              ) : (
                <>
                  <span>Se connecter</span>
                  <ArrowRight size={18} aria-hidden="true" />
                </>
              )}
            </button>
          </form>

          <p className="login-security-note">
            <Lock size={12} aria-hidden="true" />
            Connexion chiffrée — accès réservé aux administrateurs
          </p>
        </div>
      </section>
    </main>
  );
};
