# --- Étape 1 : build de la PWA ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# --- Étape 2 : service via nginx ---
FROM nginx:1.27-alpine
# Les fichiers sont servis sous /olympus (cf. base Vite et nginx.conf).
COPY --from=build /app/dist /usr/share/nginx/html/olympus
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/olympus/ >/dev/null 2>&1 || exit 1
