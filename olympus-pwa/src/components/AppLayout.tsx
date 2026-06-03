import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { BottomNav } from "./BottomNav";
import { FullPageSpinner } from "./ui/misc";

// Conteneur principal mobile-first avec marge basse pour la nav flottante.
export function AppLayout() {
  return (
    <div className="mx-auto min-h-dvh max-w-lg" style={{ paddingTop: "env(safe-area-inset-top)" }}>
      <main className="page-enter pb-28">
        <Suspense fallback={<FullPageSpinner />}>
          <Outlet />
        </Suspense>
      </main>
      <BottomNav />
    </div>
  );
}

// En-tête monumental réutilisable : titre serif qui « déborde ».
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
          <p className="lapidary text-[0.6rem] tracking-[0.25em] text-gold">{overline}</p>
        )}
        <h1 className="text-3xl leading-none text-marble">{title}</h1>
      </div>
      {action}
    </header>
  );
}
