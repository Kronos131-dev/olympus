import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { VitePWA } from "vite-plugin-pwa";
import { fileURLToPath, URL } from "node:url";

// La PWA Olympus est servie à la RACINE de son propre sous-domaine
// (olympus.chiron-sanctuaire.fr).
const BASE = "/";

export default defineConfig({
  base: BASE,
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: 5174,
    // En dev, on relaie les appels API vers le backend local (port exposé 8081).
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: [
        "icons/favicon-32.png",
        "icons/favicon-180.png",
        "icons/olympus-192.png",
        "icons/olympus-512.png",
        "icons/olympus-apple-touch-180.png",
      ],
      manifest: {
        name: "Olympus",
        short_name: "Olympus",
        description: "Suivi nutritionnel et conquête personnelle.",
        lang: "fr",
        scope: BASE,
        start_url: BASE,
        display: "standalone",
        orientation: "portrait",
        background_color: "#0c0f0f",
        theme_color: "#0c0f0f",
        icons: [
          { src: "icons/olympus-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
          { src: "icons/olympus-256.png", sizes: "256x256", type: "image/png", purpose: "any" },
          { src: "icons/olympus-384.png", sizes: "384x384", type: "image/png", purpose: "any" },
          { src: "icons/olympus-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
          { src: "icons/olympus-maskable-192.png", sizes: "192x192", type: "image/png", purpose: "maskable" },
          { src: "icons/olympus-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
        ],
      },
      workbox: {
        navigateFallback: "/index.html",
        // Ne jamais servir l'index pour les appels API.
        navigateFallbackDenylist: [/^\/api\//],
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          {
            // API toujours fraîche (jamais mise en cache).
            urlPattern: ({ url }) => url.pathname.startsWith("/api/"),
            handler: "NetworkOnly",
          },
          {
            // Images d'aliments (Open Food Facts) : cache long pour les perfs.
            urlPattern: ({ url }) => /openfoodfacts\.org$/.test(url.hostname),
            handler: "CacheFirst",
            options: {
              cacheName: "off-images",
              expiration: { maxEntries: 80, maxAgeSeconds: 60 * 60 * 24 * 30 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
      devOptions: { enabled: false },
    }),
  ],
});
