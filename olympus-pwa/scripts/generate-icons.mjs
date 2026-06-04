// Génère les icônes PNG d'Olympus à plusieurs tailles à partir des sources SVG.
// Usage : node scripts/generate-icons.mjs
//
// - Icônes "any" (full-bleed) : le glyphe remplit ~86% du canvas → lisible même
//   en petite taille (le problème « icône trop petite » sur mobile).
// - Icône "maskable" : glyphe maintenu dans la zone de sécurité (~80% central)
//   pour ne pas être rogné par les masques circulaires/squircle d'Android.

import sharp from "sharp";
import { readFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const iconsDir = join(here, "..", "public", "icons");
mkdirSync(iconsDir, { recursive: true });

// Source "any" full-bleed : on réutilise la favicon (déjà agrandie).
const anySvg = readFileSync(join(iconsDir, "favicon.svg"));

// Source "maskable" : glyphe à l'échelle d'origine, recentré, dans la zone sûre.
const maskableSvg = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <rect width="512" height="512" fill="#0c0f0f"/>
  <g transform="translate(0 8)" fill="none" stroke="url(#g)" stroke-width="22"
     stroke-linecap="round" stroke-linejoin="round">
    <path d="M96 200 256 104 416 200"/>
    <path d="M136 200v152M200 200v152M312 200v152M376 200v152"/>
    <path d="M96 352h320M96 392h320"/>
  </g>
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="512" y2="512" gradientUnits="userSpaceOnUse">
      <stop stop-color="#af8d11"/>
      <stop offset="0.5" stop-color="#e9c349"/>
      <stop offset="1" stop-color="#af8d11"/>
    </linearGradient>
  </defs>
</svg>`);

// [source, taille, nom de fichier]
const targets = [
  // Icônes standard "any" — plusieurs tailles pour couvrir tous les appareils.
  [anySvg, 192, "olympus-192.png"],
  [anySvg, 256, "olympus-256.png"],
  [anySvg, 384, "olympus-384.png"],
  [anySvg, 512, "olympus-512.png"],
  // apple-touch-icon (iOS recommande 180×180).
  [anySvg, 180, "olympus-apple-touch-180.png"],
  // Maskable (Android adaptive icons).
  [maskableSvg, 192, "olympus-maskable-192.png"],
  [maskableSvg, 512, "olympus-maskable-512.png"],
];

await Promise.all(
  targets.map(async ([svg, size, name]) => {
    await sharp(svg, { density: 384 })
      .resize(size, size, { fit: "contain", background: { r: 12, g: 15, b: 15, alpha: 1 } })
      .png()
      .toFile(join(iconsDir, name));
    console.log(`✓ ${name} (${size}×${size})`);
  }),
);

console.log("Icônes générées dans public/icons/");
