import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { VitePWA } from "vite-plugin-pwa";
import { fileURLToPath, URL } from "node:url";

// La PWA est servie sous le sous-chemin /olympus/ derrière le reverse-proxy Chiron.
const BASE = "/olympus/";

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
      "/olympus/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/olympus\/api/, "/api"),
      },
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["icons/favicon.svg"],
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
          { src: "icons/favicon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
          { src: "icons/favicon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
        ],
      },
      workbox: {
        navigateFallback: `${BASE}index.html`,
        // Ne jamais mettre en cache les appels API : on veut des données fraîches.
        navigateFallbackDenylist: [/^\/olympus\/api\//],
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.startsWith("/olympus/api/"),
            handler: "NetworkOnly",
          },
        ],
      },
      devOptions: { enabled: false },
    }),
  ],
});
