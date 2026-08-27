# Frontend — Référentiel Technique Clients

React SPA for the client technical repository: client records, installed modules, a
credentials vault, and screenshots attached to each client.

The backend's API contract is documented separately and interactively in **Swagger UI**:

- Docker stack → <http://localhost:8083/swagger-ui.html> (`BACKEND_PORT` in the root `.env`)
- `mvnw spring-boot:run` locally → <http://localhost:8082/swagger-ui.html>

The raw OpenAPI 3 document is at `/v3/api-docs` on the same host. This file covers what
Swagger cannot describe: how this app is put together, and why.

---

## Stack

| | |
|---|---|
| React 19 + TypeScript | strict mode, no class components |
| Vite 8 | dev server + build (requires Node ≥ 20.19 or ≥ 22.12) |
| React Router 6 | client-side routing |
| lucide-react | icon set |
| Plain CSS | one stylesheet, `src/styles.css` — no CSS-in-JS, no component libraries |

Tailwind and axios appear in the **root** `package.json` but are not used by this app;
the frontend's own `package.json` is the authoritative dependency list.

---

## Running it

### Against the Docker stack (nothing else to start)

```bash
docker compose up -d --build      # from the repository root
```

Open **http://localhost:8081**. nginx serves the built bundle and proxies `/api` to the
backend container.

### Dev server with hot reload

```bash
cd frontend
npm install
npm run dev                       # http://localhost:5173
```

`vite.config.ts` proxies `/api` to `http://localhost:8082`, so a backend must be running
there — `cd repository-api && ./mvnw spring-boot:run`, or `npm run dev` from the
repository root to start both at once.

The dev backend stores data in a local H2 **file** (`repository-api/data/`), so what you
enter survives a restart. It used to be `jdbc:h2:mem:`, which discarded everything on
shutdown — and was easy to miss, because `DemoDataInitializer` re-seeded its demo clients
on every boot, so the list looked intact while anything you had actually created (uploaded
images above all) was gone.

For a clean slate: stop the backend and delete `repository-api/data/`. It is recreated,
and reseeded, on the next start.

To point the dev server at a backend somewhere else, set `VITE_API_URL`:

```bash
VITE_API_URL=http://localhost:8083/api npm run dev
```

Note this bypasses the Vite proxy and makes the calls cross-origin, so the backend needs
that origin in `CORS_ALLOWED_ORIGINS`.

### Other scripts

| Command | Purpose |
|---|---|
| `npm run build` | type-check (`tsc -b`) then bundle to `dist/` |
| `npm run preview` | serve the production build locally |
| `npm run lint` | ESLint over the project |

---

## Structure

```
src/
├── main.tsx                  entry point — mounts <App>
├── App.tsx                   router + <AuthProvider> + <ProtectedRoute>
├── styles.css                the entire design system (see "Styling")
│
├── pages/
│   └── LoginPage.tsx         unauthenticated route
│
├── components/
│   ├── Dashboard/
│   │   ├── Dashboard.tsx     client list screen — search, sort
│   │   └── ClientTable.tsx   sortable table
│   ├── ClientDetail/
│   │   ├── ClientDetail.tsx  screen container — owns all data + mutations
│   │   ├── ModuleList.tsx    installed-modules table
│   │   ├── ModuleFormModal.tsx  create/edit module dialog
│   │   ├── AccessVault.tsx   the four fixed credential rows + notes
│   │   ├── access-types.ts   which fields each access type shows
│   │   └── AccessImageGallery.tsx  upload + thumbnail grid + lightbox
│   └── shared/
│       ├── StatusBadge.tsx   en regle / suspendu pill
│       ├── ErrorBanner.tsx   inline error
│       └── Toast.tsx         transient confirmation
│
├── context/
│   ├── auth-context.ts       React context + useAuth() hook
│   └── AuthProvider.tsx      token state, localStorage persistence
│
├── services/
│   └── api.ts                every HTTP call in the app
│
├── types/
│   └── models.ts             TypeScript mirrors of the backend DTOs
│
└── lib/
    └── jwt.ts                token expiry check (decode only, no verification)
```

### The shape of a screen

Screen containers (`Dashboard`, `ClientDetail`) own **all** data fetching and mutations.
Everything below them is presentational: it receives data and callbacks as props and
holds only its own UI state (which row is being edited, whether a modal is open).

`AccessImageGallery` is the deliberate exception — it fetches, uploads and deletes its
own images. Uploads commit immediately rather than participating in the vault's
Modifier/Terminer draft cycle, so there is nothing for a parent to stage on its behalf.

---

## Routing

Defined in `App.tsx`:

| Path | Component | Access |
|---|---|---|
| `/login` | `LoginPage` | public |
| `/` | redirect | → `/dashboard` if authenticated, else `/login` |
| `/dashboard` | `Dashboard` | protected |
| `/clients/:clientId` | `ClientDetail` | protected |
| `*` | redirect to `/` | — |

`ProtectedRoute` redirects unauthenticated visitors to `/login`, passing the attempted
path in router state so `LoginPage` can send them back there after signing in.

Deep links work on refresh because nginx falls back to `index.html` for any path that
isn't a real file (`try_files $uri $uri/ /index.html`). Serving `dist/` with a static
server that lacks that rule will 404 on `/clients/7`.

---

## Authentication

1. `LoginPage` posts to `/api/auth/login` and receives `{ accessToken, username, role }`.
2. `AuthProvider` stores it in state and in `localStorage` under `referentiel-auth`.
3. Every subsequent call passes the token as `Authorization: Bearer <token>`.

**Tokens last 15 minutes and there is no refresh token.** Two mechanisms handle expiry:

- On mount, `AuthProvider` checks `isJwtExpired()` (`lib/jwt.ts`) and discards a stale
  token rather than rendering a signed-in UI that cannot fetch anything.
- At runtime, `api.ts` calls the handler registered via `setUnauthorizedHandler()` on any
  `401` from a request that *carried* a token — which logs the user out. The
  "carried a token" condition is what stops a failed login attempt from triggering it.

`lib/jwt.ts` only base64-decodes the payload to read `exp`. It does **not** verify the
signature — it cannot, the key is server-side. It is a UX optimisation, never a security
check; the server re-validates every request regardless.

---

## The API layer

`services/api.ts` is the only module that calls `fetch`. Components never construct URLs.

Two transports share one pipeline:

- `request<T>()` — JSON responses, handles `204 No Content`
- `rawRequest()` — returns the raw `Response`, used for binary payloads

Both attach the bearer token, trigger the 401 logout hook, and convert non-2xx responses
into an `ApiError`.

### ApiError

```ts
class ApiError extends Error {
  status: number;
  fromServer: boolean;   // true = message came from the API's JSON body
}
```

`fromServer` exists because callers rendering an error to a user need to tell
`"Le nom du client est requis"` (worth showing) from `"Request failed (403)"` (a status
code dressed up as a sentence). Only show `err.message` when `fromServer` is true;
otherwise substitute your own copy.

### Loading authenticated images

`GET …/access-images/{id}/content` requires the bearer token, and **an `<img src>` cannot
send headers** — it would get a 401. So `AccessImageGallery`:

1. calls `fetchAccessImageBlob()` with the token,
2. wraps the response in `URL.createObjectURL()`,
3. renders that `blob:` URL,
4. and calls `URL.revokeObjectURL()` on unmount.

Skipping step 4 pins every decoded bitmap in memory for the life of the document. The
alternative designs — a public endpoint, or a token in the query string — would leak
client infrastructure screenshots into proxy logs and browser history.

---

## Types

`types/models.ts` mirrors the backend DTOs by hand. They are **not** generated, so a
backend field rename will compile fine here and fail at runtime. When the API changes,
check this file against Swagger.

Worth knowing:

- `ClientStatut` is `"en regle" | "suspendu"` — the `@JsonValue` spelling, unaccented.
  Not `EN_REGLE`.
- Dates are ISO strings (`"2026-06-14"`), which feed `<input type="date">` directly.
- `TechnicalAccessDto.password` arrives **in clear text**. It is encrypted at rest and
  decrypted on read — the point of a credentials vault. Never log it, never put it in a
  URL, never send it to analytics.

---

## Styling

One stylesheet, `src/styles.css`, organised by feature with banner comments. No CSS
modules, no utility framework, no scoping — class names are the contract, so prefix new
ones by feature (`gallery-`, `access-`, `login-`) to avoid collisions.

Design tokens are literal values rather than CSS variables, following the existing file:

| Role | Value |
|---|---|
| Page background | `#f4f6f9` |
| Card / surface | `#ffffff`, radius `10px`, `0 1px 3px rgba(0,0,0,.06)` |
| Text primary / secondary / muted | `#1e293b` / `#64748b` / `#94a3b8` |
| Border | `#e2e8f0` |
| Accent (primary action) | `#2563eb` |
| Focus ring | `#93c5fd` |
| Danger | `#dc2626` |

The login screen deliberately uses a different, warmer palette (teal `#174c4a`, terracotta
`#d26e53`) — it is a standalone full-bleed screen, not part of the app shell.

Add a `prefers-reduced-motion` block for anything that animates.

---

## Gotchas worth knowing before you change things

**`body` has a global `padding: 32px`.** Any full-bleed screen must escape it — the login
page uses `position: fixed; inset: 0`. Using `min-height: 100vh` inside the padded body
gives `100vh + 64px` and a permanent scrollbar.

**nginx `add_header` does not inherit** into a `location` block that declares its own. The
security headers are therefore repeated in each block of `nginx.conf` that sets any
header. Adding a new `location` with an `add_header` silently drops them for those paths.

**nginx clears the `Origin` header on `/api/` requests.** Browsers attach `Origin` to
same-origin POSTs too, and Spring treats any request carrying it as a CORS request,
checking it against an allow-list that is empty in production — which made every browser
login fail with `403 "Invalid CORS request"` while curl worked. Do not remove
`proxy_set_header Origin "";` unless you also configure `CORS_ALLOWED_ORIGINS`.

**Upload size is capped in three places** and they must stay in step:
`client_max_body_size 6m` (nginx) ≥ `spring.servlet.multipart.max-request-size=6MB`
≥ the 5 MB per-file limit enforced in `AccessImageService`. Raising only the app limit
means nginx rejects the request before Spring ever sees it.

**Access types are fixed in the UI, free-text in the API.** `access-types.ts` hardcodes
four types (LogMeIn, SQL Server, Admin Access, VPN Accès) and `ClientDetail` auto-creates
any that a client is missing. The backend imposes no such set — `type` is just a string.

---

## Building for production

```bash
npm run build          # -> dist/
```

`frontend/Dockerfile` does this in a Node 22 stage and copies `dist/` into
`nginx:1.27-alpine`. `.dockerignore` excludes `node_modules` — without it, `COPY . .`
would overwrite the container's Linux modules with the host's, which on Windows means
platform-specific binaries (esbuild, rollup) inside a Linux image.

Vite emits content-hashed filenames, so `/assets/*` is served `immutable` for a year while
`index.html` is `no-store` — a cached `index.html` would reference bundle names that no
longer exist after a deploy.
