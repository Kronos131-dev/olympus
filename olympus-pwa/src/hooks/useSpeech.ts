import { useCallback, useEffect, useRef, useState } from "react";

export type SpeechErrorKind = "denied" | "unavailable" | null;

// Reconnaissance vocale via la Web Speech API (push-to-talk). Renvoie le transcript.
// WHY: le recognizer est construit dans un effet dont les dépendances DOIVENT rester stables.
// onResult vit dans une ref plutôt que dans le tableau de dépendances : sinon, chaque render
// déclenché par setListening(true) recrée l'effet, dont le cleanup appelle rec.abort() sur le
// recognizer qui vient tout juste de démarrer — le bouton reste doré sans qu'aucun résultat
// ne puisse jamais arriver (le nouveau recognizer créé à la place n'est, lui, jamais démarré).
export function useSpeech(onResult: (text: string) => void, lang = "fr-FR") {
  const recognitionRef = useRef<any>(null);
  const onResultRef = useRef(onResult);
  onResultRef.current = onResult;

  const [listening, setListening] = useState(false);
  const [supported, setSupported] = useState(false);
  const [error, setError] = useState<SpeechErrorKind>(null);

  useEffect(() => {
    const SR =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR || !window.isSecureContext) return;
    setSupported(true);
    const rec = new SR();
    rec.lang = lang;
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onstart = () => setError(null);
    rec.onresult = (e: any) => {
      const text = e.results[0]?.[0]?.transcript;
      if (text) onResultRef.current(text);
    };
    rec.onend = () => setListening(false);
    rec.onerror = (e: any) => {
      setListening(false);
      setError(e?.error === "not-allowed" || e?.error === "service-not-allowed" ? "denied" : "unavailable");
    };
    recognitionRef.current = rec;
    return () => rec.abort?.();
  }, [lang]);

  const toggle = useCallback(() => {
    const rec = recognitionRef.current;
    if (!rec) return;
    if (listening) {
      rec.stop();
      setListening(false);
    } else {
      setError(null);
      try {
        rec.start();
        setListening(true);
      } catch {
        setError("unavailable");
      }
    }
  }, [listening]);

  return { listening, supported, error, toggle };
}
