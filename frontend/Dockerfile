# ── Stage 1: Build ──────────────────────────────────────
# Node 22 rather than 20: Vite 8 requires Node >=20.19 || >=22.12, and pinning to
# the same major the project is developed on avoids a toolchain that builds here
# but not locally.
FROM node:22-alpine AS build

WORKDIR /app

# Manifests first — cached unless dependencies actually change.
# npm ci (not npm install) installs exactly what package-lock.json pins, so the
# image is reproducible and cannot silently drift to a newer transitive version.
COPY package.json package-lock.json ./
RUN npm ci

# Relies on .dockerignore excluding node_modules — otherwise this line copies the
# host's (Windows) modules straight over the Linux ones installed above.
COPY . .
RUN npm run build

# ── Stage 2: Serve ───────────────────────────────────────
# Pinned minor rather than :alpine, so a rebuild months from now doesn't quietly
# pick up a new nginx with different defaults.
FROM nginx:1.27-alpine

# Drop nginx's sample page so nothing of it can be served.
RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist /usr/share/nginx/html

# Static hosting + SPA history fallback + reverse proxy to the backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
