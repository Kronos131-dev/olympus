import { useEffect, useRef, useState } from "react";
import { BrowserMultiFormatReader } from "@zxing/browser";
import { Modal } from "./ui/Modal";
import { Spinner } from "./ui/misc";

interface Props {
  open: boolean;
  onClose: () => void;
  onDetected: (barcode: string) => void;
}

// Scanner de code-barres : BarcodeDetector natif si dispo, sinon @zxing/browser.
export function BarcodeScanner({ open, onClose, onDetected }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!open) return;
    let stream: MediaStream | null = null;
    let stopZxing: (() => void) | null = null;
    let cancelled = false;
    let rafId = 0;

    const handle = (code: string) => {
      if (cancelled) return;
      cancelled = true;
      onDetected(code);
      onClose();
    };

    (async () => {
      try {
        const NativeDetector = (window as unknown as { BarcodeDetector?: any }).BarcodeDetector;
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: "environment" },
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
          setReady(true);
        }

        if (NativeDetector) {
          const detector = new NativeDetector({
            formats: ["ean_13", "ean_8", "upc_a", "upc_e", "code_128"],
          });
          const tick = async () => {
            if (cancelled || !videoRef.current) return;
            try {
              const codes = await detector.detect(videoRef.current);
              if (codes[0]?.rawValue) return handle(codes[0].rawValue);
            } catch {
              /* frame non décodable */
            }
            rafId = requestAnimationFrame(tick);
          };
          rafId = requestAnimationFrame(tick);
        } else {
          const reader = new BrowserMultiFormatReader();
          const controls = await reader.decodeFromVideoElement(videoRef.current!, (result) => {
            if (result) handle(result.getText());
          });
          stopZxing = () => controls.stop();
        }
      } catch {
        setError("Caméra inaccessible. Vérifie les autorisations.");
      }
    })();

    return () => {
      cancelled = true;
      cancelAnimationFrame(rafId);
      stopZxing?.();
      stream?.getTracks().forEach((t) => t.stop());
      setReady(false);
    };
  }, [open, onClose, onDetected]);

  return (
    <Modal open={open} onClose={onClose} title="Scanner un code-barres">
      {error ? (
        <p className="text-sm text-[var(--color-danger)]">{error}</p>
      ) : (
        <div className="relative aspect-square w-full overflow-hidden bg-black">
          <video ref={videoRef} className="size-full object-cover" muted playsInline />
          {!ready && (
            <div className="absolute inset-0 grid place-items-center">
              <Spinner />
            </div>
          )}
          <div className="pointer-events-none absolute inset-x-8 top-1/2 h-0.5 -translate-y-1/2 bg-gold/80" />
        </div>
      )}
    </Modal>
  );
}
