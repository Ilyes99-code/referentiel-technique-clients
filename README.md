# Référentiel Technique Clients

Internal platform for tracking client technical information: client records, installed
modules, a credentials vault (passwords encrypted at rest), and screenshots attached to
each client.

| | |
|---|---|
| **Backend** | Spring Boot 3.3 · Java 21 · PostgreSQL · Flyway · Spring Security (JWT) |
| **Frontend** | React 19 · TypeScript · Vite · React Router |
| **Deployment** | Docker Compose — Postgres + API + nginx |

---

## Quickstart

**Prerequisite: [Docker Desktop](https://www.docker.com/products/docker-desktop/), running.**
Nothing else — no JDK, no Node, no local PostgreSQL.

```bash
git clone <repository-url>
cd referentiel_technique_clients
cp .env.example .env        # then edit it — see below
docker compose up -d --build
```

Then open **http://localhost:8080** and log in with the `BOOTSTRAP_ADMIN_USERNAME` /
`BOOTSTRAP_ADMIN_PASSWORD` you set in `.env`.

> **The first build takes about five minutes** — it downloads the Maven dependency tree,
> runs `npm ci`, and pulls three base images. There is no progress output during the long
> Maven step; it has not frozen. Subsequent builds take seconds.

### Filling in `.env`

`.env` is deliberately **not** in the repository — it holds the key that signs login
tokens and the key that encrypts every stored credential. Copy the template and fill it
in. Only two values have a required format:

```bash
openssl rand -base64 32     # run twice: once for JWT_SECRET, once for CREDENTIALS_SECRET
```

Both must decode to exactly 32 bytes; the application refuses to start otherwise rather
than run with a weak key. Everything else (`DB_NAME`, `DB_USER`, `DB_PASSWORD`, the admin
username and password) is yours to choose.

> ⚠️ **`CREDENTIALS_SECRET` is effectively permanent.** It is the AES-256-GCM key for
> stored technical-access passwords. Change it after data exists and every stored password
> becomes undecryptable — the ciphertext was produced with the old key and there is no
> recovery. `JWT_SECRET` is safe to rotate at any time; the only effect is that current
> sessions need to log in again.

---

## What runs where

| Service | Container | Host port | Purpose |
|---|---|---|---|
| `frontend` | nginx serving the React build | **8080** | the application |
| `backend` | Spring Boot API | 8082 | direct API access, Swagger |
| `db` | PostgreSQL 16 | 5432 | database tools (DBeaver, pgAdmin) |

Only **8080** is needed to use the app; the other two are exposed for debugging and can be
removed for a real deployment.

Inside the Compose network the containers address each other by service name — nginx
proxies `/api` to `http://backend:8082`, and the backend connects to
`jdbc:postgresql://db:5432`. Those internal ports never change, whatever you map on the
host side.

### Changing host ports

If 8080, 8082 or 5432 is already in use on your machine, uncomment and edit the port block
at the bottom of `.env`:

```properties
FRONTEND_PORT=8081
BACKEND_PORT=8083
DB_PORT=5433
```

---

## Everyday commands

```bash
docker compose up -d --build     # start (rebuilding images)
docker compose ps                # state and health of each service
docker compose logs -f backend   # follow one service's logs
docker compose down              # stop — the database is kept
docker compose down -v           # stop AND erase the database
```

`down` versus `down -v` is the only distinction that matters for your data: the `-v`
removes the named volume where PostgreSQL stores everything.

---

## Documentation

**API — Swagger UI:** <http://localhost:8082/swagger-ui.html> (raw OpenAPI at
`/v3/api-docs`). All endpoints except login require a token: call `POST /api/auth/login`,
copy `accessToken` from the response, then paste it into the **Authorize** button.

Swagger is **disabled by default in production** — it cannot be put behind a login, since
the page must load before you can authenticate through it, and it publishes the full
endpoint inventory. `docker-compose.yml` sets `SPRINGDOC_ENABLED=true` for local use; set
it to `false` for anything internet-facing.

**Frontend architecture:** [`frontend/README.md`](frontend/README.md) — structure,
routing, the auth lifecycle, the API layer, styling conventions, and the non-obvious
constraints worth knowing before changing anything.

---

## Local development without Docker

Only needed if you want hot reload. Requires **JDK 21**, **Maven** and **Node 22**.

```bash
npm install          # installs concurrently, the launcher only
cd frontend && npm install && cd ..
npm run dev          # backend on :8082, Vite on :5173
```

This runs the backend under the `dev` profile: a **file-backed H2** database in
`repository-api/data/`, not PostgreSQL. Data survives restarts; delete that folder for a
clean slate. The `dev` profile also seeds demo clients on an empty database, which the
`prod` profile deliberately does not.

---

## Notes

**No demo data in Docker.** The Compose stack runs the `prod` profile, so the dashboard is
empty on first login — only the admin account is created. That is expected, not a fault.

**Schema changes go through Flyway.** Migrations live in
`repository-api/src/main/resources/db/migration` and run automatically at startup, tracked
in a `flyway_schema_history` table. Never edit an applied migration — add a new one.
Hibernate runs in `validate` mode in production, so a mismatch between the entities and the
schema fails at startup rather than corrupting data.

**Windows: "Filename too long" while cloning.** Paths in this repository reach 100
characters, and Windows caps at 260, so this only appears when cloning into an already-deep
directory. Fix with `git config --global core.longpaths true`, or clone somewhere shallow.

**Not production-ready as-is.** The structure is sound — multi-stage builds, non-root
container user, health checks, externalised secrets, Flyway-managed schema. What is
missing: HTTPS, removing the exposed database port, `SPRINGDOC_ENABLED=false`, and real
secret management in place of a `.env` file.
