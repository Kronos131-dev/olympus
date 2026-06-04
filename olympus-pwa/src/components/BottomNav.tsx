import { NavLink } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  IconHome,
  IconOracle,
  IconMeals,
  IconPlan,
  IconProfile,
} from "./icons";

const items = [
  { to: "/", label: "Accueil", Icon: IconHome, end: true },
  { to: "/oracle", label: "Oracle", Icon: IconOracle },
  { to: "/meals", label: "Repas", Icon: IconMeals },
  { to: "/plan", label: "Planning", Icon: IconPlan },
  { to: "/profile", label: "Profil", Icon: IconProfile },
];

// Ordre des onglets, partagé avec la navigation par swipe (AppLayout).
export const TAB_PATHS = items.map((i) => i.to);

// Barre flottante en verre dépoli. Onglet actif = pastille or sobre.
export function BottomNav() {
  return (
    <nav
      className="glass fixed inset-x-0 bottom-0 z-40 border-t border-outline/20"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <ul className="mx-auto flex max-w-lg items-stretch justify-between px-2 py-1.5">
        {items.map(({ to, label, Icon, end }) => (
          <li key={to} className="flex-1">
            <NavLink
              to={to}
              end={end}
              className="flex flex-col items-center gap-1 py-1.5"
            >
              {({ isActive }) => (
                <>
                  <span
                    className={cn(
                      "flex h-8 w-12 items-center justify-center rounded-full transition-colors",
                      isActive ? "bg-gold/15 text-gold" : "text-marble-dim",
                    )}
                  >
                    <Icon size={21} />
                  </span>
                  <span
                    className={cn(
                      "whitespace-nowrap text-[0.6rem] font-medium transition-colors",
                      isActive ? "text-gold" : "text-marble-dim",
                    )}
                  >
                    {label}
                  </span>
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
