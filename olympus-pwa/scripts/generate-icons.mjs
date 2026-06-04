// Génère les icônes PNG d'Olympus à plusieurs tailles à partir du médaillon discobole IMPERIUM.
// Usage : node scripts/generate-icons.mjs
//
// Source : public/icons/source-medallion.png (sceau circulaire doré sur fond sombre, 512×512),
// repris de l'app Android OlympusFront.
//
// - Icônes "any" : le médaillon (fond sombre carré) remplit le canvas.
// - Icône "maskable" : le médaillon étant circulaire, on le réduit dans la zone de sécurité
//   (~80% central) sur fond #0c0f0f pour qu'il ne soit pas rogné par les masques Android.
// - favicon : 32 et 180 px.

import sharp from "sharp";
import { mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const iconsDir = join(here, "..", "public", "icons");
mkdirSync(iconsDir, { recursive: true });

const SOURCE = join(iconsDir, "source-medallion.png");
const BG = { r: 12, g: 15, b: 15, alpha: 1 }; // #0c0f0f

// Médaillon plein cadre, redimensionné à la taille cible.
function anyIcon(size, name) {
  return sharp(SOURCE)
    .resize(size, size, { fit: "cover" })
    .png()
    .toFile(join(iconsDir, name))
    .then(() => console.log(`✓ ${name} (${size}×${size})`));
}

// Médaillon réduit à ~80% et posé sur fond sombre → zone de sécurité maskable.
async function maskableIcon(size, name) {
  const inner = Math.round(size * 0.8);
  const badge = await sharp(SOURCE).resize(inner, inner, { fit: "cover" }).png().toBuffer();
  return sharp({ create: { width: size, height: size, channels: 4, background: BG } })
    .composite([{ input: badge, gravity: "center" }])
    .png()
    .toFile(join(iconsDir, name))
    .then(() => console.log(`✓ ${name} (${size}×${size}, maskable)`));
}

await Promise.all([
  // Icônes standard "any" — plusieurs tailles pour couvrir tous les appareils.
  anyIcon(192, "olympus-192.png"),
  anyIcon(256, "olympus-256.png"),
  anyIcon(384, "olympus-384.png"),
  anyIcon(512, "olympus-512.png"),
  // apple-touch-icon (iOS recommande 180×180).
  anyIcon(180, "olympus-apple-touch-180.png"),
  // favicons.
  anyIcon(32, "favicon-32.png"),
  anyIcon(180, "favicon-180.png"),
  // Maskable (Android adaptive icons).
  maskableIcon(192, "olympus-maskable-192.png"),
  maskableIcon(512, "olympus-maskable-512.png"),
]);

console.log("Icônes générées dans public/icons/");
