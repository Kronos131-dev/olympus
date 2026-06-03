# Olympus PWA

Front React (Vite + TypeScript) d'Olympus, conçu comme une **PWA** et servi sous
`https://chiron-sanctuaire.duckdns.org/olympus`, intégré à l'application **Chiron**.

Design « Digital Pantheon » (cf. `../DESIGN.md`) : pourpre impérial, or, marbre, angles vifs.

## Développement

```bash
npm install
npm run dev   # http://localhost:5174/olympus/
```

Le serveur de dev relaie `/olympus/api/*` vers le backend local `http://localhost:8081`
(cf. `server.proxy` dans `vite.config.ts`). Lance d'abord le backend
(`cd ../olympus-back && docker compose up`).

```bash
npm run build   # tsc -b + build Vite (sortie dans dist/)
npm run lint    # typecheck seul
```

## Authentification

- **Compte lié à Chiron** → entrée directe. Chiron ouvre la PWA avec le token de liaison
  dans le fragment d'URL : `…/olympus/#ctk=<linkToken>`. La PWA l'échange contre une
  session Olympus complète via `POST /api/v1/integration/chiron/session`
  (en-tête `X-Integration-Token`), puis nettoie l'URL. Voir `src/lib/auth/handoff.ts`.
- **Sinon** → page de connexion/inscription dédiée (`/auth/login`, `/auth/register`).
- Le JWT est rafraîchi automatiquement et de façon transparente (file d'attente sur 401).

> **À faire côté Chiron (hors de ce dépôt)** : quand un lien actif existe, l'entrée de menu
> « Olympus » doit pointer vers `…/olympus/#ctk=<linkToken>`. Et le reverse-proxy de Chiron
> (Nginx Proxy Manager) doit router `chiron-sanctuaire.duckdns.org/olympus` vers le service
> `olympus-pwa:80`.

## Déploiement

Le déploiement d'Olympus (back + PWA) est piloté par la **pipeline Chiron**
(`Chiron/.github/workflows/deploy.yml`), qui build les artefacts, les dépose par scp
sur le serveur et recrée les conteneurs. Olympus n'a plus de pipeline propre.

- **Dev** : `nginx.conf` sert la PWA sous `/olympus` et relaie `/olympus/api` vers
  `olympus-api:8080` (même origine, aucun CORS). `../olympus-back/docker-compose.yml`
  build l'image localement (`Dockerfile`).
- **Prod** (`../olympus-back/docker-compose.prod.yml`, modèle « artefacts montés ») :
  l'image `nginx` monte le `dist/` (PWA) et `nginx.conf` ; l'API monte le JAR. La pipeline
  Chiron dépose `olympus.jar`, `olympus-pwa-dist/` et `olympus-pwa-nginx.conf` dans
  `~/olympus` sur le serveur.

## Structure

```
src/
  lib/api/      client fetch (refresh JWT), endpoints typés, token store
  lib/auth/     AuthContext + handoff Chiron
  hooks/        TanStack Query (queries.ts), useDebounce, useSpeech
  components/   design system (ui/), BottomNav, AppLayout, FoodFinder, BarcodeScanner, icons
  pages/        Login, Register, Dashboard, Oracle, Meals, MealEditor, AddFood, Plan, Stats, Profile
  types/api.ts  miroir des DTO/enums backend
```
