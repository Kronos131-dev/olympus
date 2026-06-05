import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { fr, type Dict } from "./fr";
import { en } from "./en";

export type Lang = "fr" | "en";
export type { Dict };

const DICTS: Record<Lang, Dict> = { fr, en };
const STORAGE_KEY = "olympus.lang";

/** Langue persistée, sinon déduite du navigateur (FR par défaut). */
function detectLang(): Lang {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === "fr" || stored === "en") return stored;
  } catch {
    /* localStorage indisponible */
  }
  return typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("en")
    ? "en"
    : "fr";
}

/** Locale Intl correspondant à la langue (dates, nombres). */
export function localeFor(lang: Lang): string {
  return lang === "en" ? "en-US" : "fr-FR";
}

/** Dictionnaire courant hors React (pour les composants classe / fallback d'erreur). */
export function getDict(): Dict {
  return DICTS[detectLang()];
}

interface LanguageState {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: Dict;
}

const LanguageCtx = createContext<LanguageState | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(() => detectLang());

  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);

  const setLang = useCallback((next: Lang) => {
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* ignore */
    }
    setLangState(next);
  }, []);

  const value = useMemo<LanguageState>(
    () => ({ lang, setLang, t: DICTS[lang] }),
    [lang, setLang],
  );

  return <LanguageCtx.Provider value={value}>{children}</LanguageCtx.Provider>;
}

function useLanguageCtx(): LanguageState {
  const ctx = useContext(LanguageCtx);
  if (!ctx) throw new Error("useT/useLang doit être utilisé dans LanguageProvider");
  return ctx;
}

/** Dictionnaire de la langue active. Usage : `const t = useT(); t.auth.login` */
export function useT(): Dict {
  return useLanguageCtx().t;
}

/** Langue active + setter (pour le sélecteur de langue). */
export function useLang(): { lang: Lang; setLang: (lang: Lang) => void } {
  const { lang, setLang } = useLanguageCtx();
  return { lang, setLang };
}
