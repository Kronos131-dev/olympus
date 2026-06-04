import { Suspense, useRef } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { BottomNav, TAB_PATHS } from "./BottomNav";
import { FullPageSpinner } from "./ui/misc";

// Conteneur principal mobile-first. Navigation par swipe gauche/droite entre les onglets.
export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const touch = useRef<{ x: number; y: number } | null>(null);

  const onTouchStart = (e: React.TouchEvent) => {
    const t = e.touches[0];
    touch.current = { x: t.clientX, y: t.clientY };
  };

  const onTouchEnd = (e: React.TouchEvent) => {
    const start = touch.current;
    touch.current = null;
    if (!start) return;
    const t = e.changedTouches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;
    // Swipe horizontal franc uniquement (on laisse le scroll vertical tranquille).
    if (Math.abs(dx) < 60 || Math.abs(dx) < Math.abs(dy) * 1.5) return;
    const idx = TAB_PATHS.indexOf(location.pathname);
    if (idx === -1) return;
    const next = dx < 0 ? idx + 1 : idx - 1;
    if (next < 0 || next >= TAB_PATHS.length) return;
    navigate(TAB_PATHS[next]);
  };

  return (
    <div className="mx-auto min-h-dvh max-w-lg" style={{ paddingTop: "env(safe-area-inset-top)" }}>
      {/* L'Oracle gère sa propre hauteur (colonne fixe) : pas de marge basse ici. */}
      <main
        className={location.pathname === "/oracle" ? "" : "pb-28"}
        onTouchStart={onTouchStart}
        onTouchEnd={onTouchEnd}
      >
        {/* key = pathname → rejoue l'animation d'entrée à chaque changement d'onglet. */}
        <div key={location.pathname} className="page-enter">
          <Suspense fallback={<FullPageSpinner />}>
            <Outlet />
          </Suspense>
        </div>
      </main>
      <BottomNav />
    </div>
  );
}

// En-tête de page réutilisable : sur-titre sobre + titre sans-serif.
export function PageHeader({
  overline,
  title,
  action,
}: {
  overline?: string;
  title: string;
  action?: React.ReactNode;
}) {
  return (
    <header className="flex items-end justify-between px-5 pt-8 pb-4">
      <div>
        {overline && (
          <p className="text-xs font-semibold tracking-wide text-gold">{overline}</p>
        )}
        <h1 className="mt-1 text-3xl leading-none text-marble">{title}</h1>
      </div>
      {action}
    </header>
  );
}
