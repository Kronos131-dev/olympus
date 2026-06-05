import { useEffect, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { agentApi } from "@/lib/api/endpoints";
import { errorMessage } from "@/lib/api/client";
import { qk, useProfile, useUpdateProfile } from "@/hooks/queries";
import { useToast } from "@/components/ui/Toast";
import { useSpeech } from "@/hooks/useSpeech";
import { compressImage } from "@/lib/image";
import { Spinner } from "@/components/ui/misc";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { IconCamera, IconClose, IconMic, IconSend, IconSparkle, IconTrash } from "@/components/icons";
import { cn } from "@/lib/utils";
import { useT } from "@/lib/i18n";
import type { AiProvider, ChatMessageDto } from "@/types/api";

interface LocalMessage {
  id: string | number;
  role: "USER" | "ASSISTANT";
  content: string;
  pending?: boolean;
  provider?: AiProvider;
}

const PROVIDER_LABEL: Record<AiProvider, string> = { MISTRAL: "Mistral", GEMINI: "Gemini" };

export default function OraclePage() {
  const tr = useT();
  const toast = useToast();
  const qc = useQueryClient();
  const profile = useProfile();
  const updateProfile = useUpdateProfile();

  const [conversationId, setConversationId] = useState<number | undefined>(undefined);
  const [messages, setMessages] = useState<LocalMessage[]>([]);
  const [input, setInput] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [sending, setSending] = useState(false);
  const [confirmClear, setConfirmClear] = useState(false);
  const [clearing, setClearing] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const { listening, supported, toggle } = useSpeech((t) =>
    setInput((prev) => (prev ? `${prev} ${t}` : t)),
  );

  // Charge la conversation la plus récente au montage.
  const conversations = useQuery({
    queryKey: ["conversations"],
    queryFn: agentApi.conversations,
  });

  useEffect(() => {
    const latest = conversations.data?.[0];
    if (latest && conversationId === undefined) {
      setConversationId(latest.id);
      agentApi.conversation(latest.id).then((msgs: ChatMessageDto[]) => {
        setMessages(
          msgs
            .filter((m) => m.role !== "SYSTEM")
            .map((m) => ({ id: m.id, role: m.role as "USER" | "ASSISTANT", content: m.content })),
        );
      });
    }
  }, [conversations.data, conversationId]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const send = async () => {
    const text = input.trim();
    if (!text && !image) return;
    setSending(true);
    const tempId = `tmp-${Date.now()}`;
    setMessages((m) => [
      ...m,
      { id: tempId, role: "USER", content: text || `📷 ${tr.oracle.photo}` },
      { id: `${tempId}-a`, role: "ASSISTANT", content: "", pending: true },
    ]);
    setInput("");
    const photo = image;
    setImage(null);
    try {
      const blob = photo ? await compressImage(photo) : undefined;
      const res = await agentApi.chat(text, conversationId, blob);
      setConversationId(res.conversationId);
      setMessages((m) =>
        m.map((msg) =>
          msg.id === `${tempId}-a`
            ? { ...msg, content: res.reply, pending: false, provider: res.provider }
            : msg,
        ),
      );
      // Si on voulait Gemini mais que le serveur a répondu avec un autre fournisseur.
      const wanted = profile.data?.aiProvider;
      if (wanted && res.provider && res.provider !== wanted) {
        toast(tr.oracle.providerUnavailable(PROVIDER_LABEL[wanted], PROVIDER_LABEL[res.provider]), "info");
      }
      if (res.actionsTaken?.length) toast(res.actionsTaken.join(" · "), "success");
      qc.invalidateQueries({ queryKey: ["conversations"] });
      qc.invalidateQueries({ queryKey: ["dailyLog"] });
    } catch (e) {
      // Afficher la vraie cause (ex. erreur Gemini) au lieu d'un message générique muet.
      const msg = errorMessage(e, tr.oracle.silent);
      setMessages((m) =>
        m.map((mm) =>
          mm.id === `${tempId}-a` ? { ...mm, content: msg, pending: false } : mm,
        ),
      );
    } finally {
      setSending(false);
    }
  };

  const provider: AiProvider = profile.data?.aiProvider ?? "MISTRAL";
  const setProvider = (p: AiProvider) => {
    if (p === provider) return;
    // Optimiste : l'UI bascule immédiatement, sans attendre le réseau.
    if (profile.data) qc.setQueryData(qk.profile, { ...profile.data, aiProvider: p });
    updateProfile.mutate(
      { aiProvider: p },
      { onError: (e) => toast(errorMessage(e, tr.oracle.providerError), "error") },
    );
  };

  const startNew = () => {
    setConversationId(undefined);
    setMessages([]);
  };

  // Supprime la conversation courante (donc l'historique envoyé à l'IA) puis repart à zéro,
  // pour ne pas polluer le contexte de l'Oracle.
  const clearHistory = async () => {
    setClearing(true);
    try {
      if (conversationId != null) await agentApi.deleteConversation(conversationId);
      setConversationId(undefined);
      setMessages([]);
      qc.invalidateQueries({ queryKey: ["conversations"] });
      setConfirmClear(false);
      toast(tr.oracle.cleared, "success");
    } catch (e) {
      toast(errorMessage(e, tr.oracle.clearError), "error");
    } finally {
      setClearing(false);
    }
  };

  return (
    // Colonne de hauteur fixe : viewport moins la barre d'app (3.5rem), la BottomNav (5.5rem)
    // et l'encoche haute. La saisie reste ainsi au-dessus de la barre de navigation.
    <div
      className="flex flex-col"
      style={{ height: "calc(100dvh - 3.5rem - 5.5rem - env(safe-area-inset-top))" }}
    >
      <header className="flex flex-none items-center justify-between px-5 pt-7 pb-3">
        <div>
          <p className="text-xs font-semibold tracking-wide text-gold">{tr.oracle.overline}</p>
          <h1 className="text-2xl text-marble">{tr.oracle.title}</h1>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-full bg-surface-high p-0.5 text-xs font-semibold">
            {(["MISTRAL", "GEMINI"] as AiProvider[]).map((p) => (
              <button
                key={p}
                onClick={() => setProvider(p)}
                className={cn(
                  "rounded-full px-3 py-1 transition-colors",
                  provider === p ? "bg-gold text-[var(--color-on-gold)]" : "text-marble-dim",
                )}
              >
                {PROVIDER_LABEL[p]}
              </button>
            ))}
          </div>
          <button
            onClick={() => setConfirmClear(true)}
            disabled={messages.length === 0}
            className="rounded-full p-2 text-marble-dim hover:text-[var(--color-danger)] disabled:opacity-40 disabled:hover:text-marble-dim"
            aria-label={tr.oracle.clearHistory}
          >
            <IconTrash size={20} />
          </button>
          <button
            onClick={startNew}
            className="rounded-full p-2 text-marble-dim hover:text-marble"
            aria-label={tr.oracle.newConv}
          >
            <IconSparkle size={20} />
          </button>
        </div>
      </header>

      <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4">
        {messages.length === 0 && (
          <div className="grid h-full place-items-center text-center">
            <div>
              <p className="text-base font-semibold text-marble">{tr.oracle.emptyTitle}</p>
              <p className="mt-2 max-w-xs text-sm text-marble-dim/80">{tr.oracle.emptyHint}</p>
            </div>
          </div>
        )}
        {messages.map((m) => (
          <div
            key={m.id}
            className={cn("flex flex-col", m.role === "USER" ? "items-end" : "items-start")}
          >
            <div
              className={cn(
                "max-w-[82%] whitespace-pre-wrap px-4 py-2.5 text-sm",
                m.role === "USER"
                  ? "rounded-2xl rounded-br-md bg-gold text-[var(--color-on-gold)]"
                  : "rounded-2xl rounded-bl-md bg-surface-high text-marble",
              )}
            >
              {m.pending ? <Spinner className="size-4" /> : m.content}
            </div>
            {m.role === "ASSISTANT" && m.provider && !m.pending && (
              <span className="mt-1 px-1 text-[0.65rem] text-marble-dim">
                {tr.oracle.via(PROVIDER_LABEL[m.provider])}
              </span>
            )}
          </div>
        ))}
        <div ref={endRef} />
      </div>

      <div
        className="flex-none border-t border-outline/20 bg-surface px-4 pt-3"
        style={{ paddingBottom: "calc(0.75rem + env(safe-area-inset-bottom))" }}
      >
        {image && (
          <div className="mb-2 flex items-center gap-2 text-xs text-marble-dim">
            <span className="truncate">📷 {image.name}</span>
            <button onClick={() => setImage(null)} className="text-[var(--color-danger)]">
              <IconClose size={14} />
            </button>
          </div>
        )}
        <div className="flex items-end gap-1.5">
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            capture="environment"
            className="hidden"
            onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          />
          <button onClick={() => fileRef.current?.click()} className="rounded-full p-2 text-marble-dim hover:text-gold" aria-label={tr.oracle.photo}>
            <IconCamera size={22} />
          </button>
          {supported && (
            <button
              onClick={toggle}
              className={cn("rounded-full p-2", listening ? "text-gold" : "text-marble-dim hover:text-gold")}
              aria-label={tr.oracle.dictate}
            >
              <IconMic size={22} />
            </button>
          )}
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                send();
              }
            }}
            rows={1}
            placeholder={tr.oracle.placeholder}
            className="max-h-28 min-h-[2.75rem] flex-1 resize-none rounded-[var(--radius)] border border-outline/60 bg-surface-lowest px-3 py-2.5 text-sm text-marble outline-none focus:border-gold"
          />
          <button
            onClick={send}
            disabled={sending || (!input.trim() && !image)}
            className="rounded-full bg-gold p-2.5 text-[var(--color-on-gold)] transition active:scale-95 disabled:opacity-40"
            aria-label="Envoyer"
          >
            <IconSend size={20} />
          </button>
        </div>
      </div>

      <Modal
        open={confirmClear}
        onClose={() => !clearing && setConfirmClear(false)}
        title={tr.oracle.clearTitle}
      >
        <p className="mb-5 text-sm text-marble-dim">{tr.oracle.clearConfirm}</p>
        <div className="flex gap-3">
          <Button block variant="ghost" onClick={() => setConfirmClear(false)} disabled={clearing}>
            {tr.common.cancel}
          </Button>
          <Button block variant="danger" loading={clearing} onClick={clearHistory}>
            {tr.oracle.clear}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
