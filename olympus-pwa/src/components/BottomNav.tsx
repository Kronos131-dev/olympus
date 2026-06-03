import { NavLink } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  IconHome,
  IconOracle,
  IconMeals,
  IconPlan,
  IconStats,
  IconProfile,
} from "./icons";

const items = [
  { to: "/", label: "Accueil", Icon: IconHome, end: true },
  { to: "/oracle", label: "Oracle", Icon: IconOracle },
  { to: "/meals", label: "Repas", Icon: IconMeals },
  { to: "/plan", label: "Planning", Icon: IconPlan },
  { to: "/stats", label: "Stats", Icon: IconStats },
  { to: "/profile", label: "Profil", Icon: IconProfile },
];

// Barre flottante en verre dépoli. Actif = éclat marbre + accent or.
export function BottomNav() {
  return (
    <nav
      className="glass fixed inset-x-0 bottom-0 z-40 border-t border-outline/10"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <ul className="mx-auto flex max-w-lg items-stretch justify-between px-2">
        {items.map(({ to, label, Icon, end }) => (
          <li key={to} className="flex-1">
            <NavLink
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  "flex flex-col items-center gap-1 py-2.5 transition-colors",
                  isActive ? "text-gold" : "text-marble-dim hover:text-marble",
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon size={22} />
                  <span
                    className={cn(
                      "lapidary text-[0.55rem] tracking-[0.08em]",
                      isActive && "text-gold",
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
