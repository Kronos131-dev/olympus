import { useCallback, useEffect, useRef, useState } from "react";

// Reconnaissance vocale via la Web Speech API (push-to-talk). Renvoie le transcript.
export function useSpeech(onResult: (text: string) => void, lang = "fr-FR") {
  const recognitionRef = useRef<any>(null);
  const [listening, setListening] = useState(false);
  const [supported, setSupported] = useState(false);

  useEffect(() => {
    const SR =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) return;
    setSupported(true);
    const rec = new SR();
    rec.lang = lang;
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (e: any) => {
      const text = e.results[0]?.[0]?.transcript;
      if (text) onResult(text);
    };
    rec.onend = () => setListening(false);
    rec.onerror = () => setListening(false);
    recognitionRef.current = rec;
    return () => rec.abort?.();
  }, [lang, onResult]);

  const toggle = useCallback(() => {
    const rec = recognitionRef.current;
    if (!rec) return;
    if (listening) {
      rec.stop();
      setListening(false);
    } else {
      try {
        rec.start();
        setListening(true);
      } catch {
        /* déjà démarré */
      }
    }
  }, [listening]);

  return { listening, supported, toggle };
}
