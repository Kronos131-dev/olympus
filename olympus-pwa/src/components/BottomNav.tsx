import { NavLink } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n";
import {
  IconHome,
  IconOracle,
  IconMeals,
  IconPlan,
  IconProfile,
} from "./icons";

const items = [
  { to: "/", key: "home", Icon: IconHome, end: true },
  { to: "/oracle", key: "oracle", Icon: IconOracle, end: false },
  { to: "/meals", key: "meals", Icon: IconMeals, end: false },
  { to: "/plan", key: "plan", Icon: IconPlan, end: false },
  { to: "/profile", key: "profile", Icon: IconProfile, end: false },
] as const;

// Ordre des onglets, partagé avec la navigation par swipe (AppLayout).
export const TAB_PATHS: string[] = items.map((i) => i.to);

// Barre flottante en verre dépoli. Onglet actif = pastille or sobre.
export function BottomNav() {
  const t = useT();
  return (
    <nav
      className="glass fixed inset-x-0 bottom-0 z-40 border-t border-outline/20"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
    >
      <ul className="mx-auto flex max-w-lg items-stretch justify-between px-2 py-1.5">
        {items.map(({ to, key, Icon, end }) => (
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
                    {t.nav[key]}
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
