import { useEffect, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { agentApi } from "@/lib/api/endpoints";
import { useProfile, useUpdateProfile } from "@/hooks/queries";
import { useToast } from "@/components/ui/Toast";
import { useSpeech } from "@/hooks/useSpeech";
import { compressImage } from "@/lib/image";
import { Spinner } from "@/components/ui/misc";
import { IconCamera, IconClose, IconMic, IconSend, IconSparkle } from "@/components/icons";
import { cn } from "@/lib/utils";
import type { AiProvider, ChatMessageDto } from "@/types/api";

interface LocalMessage {
  id: string | number;
  role: "USER" | "ASSISTANT";
  content: string;
  pending?: boolean;
}

export default function OraclePage() {
  const toast = useToast();
  const qc = useQueryClient();
  const profile = useProfile();
  const updateProfile = useUpdateProfile();

  const [conversationId, setConversationId] = useState<number | undefined>(undefined);
  const [messages, setMessages] = useState<LocalMessage[]>([]);
  const [input, setInput] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [sending, setSending] = useState(false);
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
      { id: tempId, role: "USER", content: text || "📷 Photo" },
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
            ? { ...msg, content: res.reply, pending: false }
            : msg,
        ),
      );
      if (res.actionsTaken?.length) toast(res.actionsTaken.join(" · "), "success");
      qc.invalidateQueries({ queryKey: ["conversations"] });
      qc.invalidateQueries({ queryKey: ["dailyLog"] });
    } catch {
      setMessages((m) =>
        m.map((msg) =>
          msg.id === `${tempId}-a`
            ? { ...msg, content: "L'Oracle est resté silencieux. Réessaie.", pending: false }
            : msg,
        ),
      );
    } finally {
      setSending(false);
    }
  };

  const toggleProvider = () => {
    const next: AiProvider = profile.data?.aiProvider === "MISTRAL" ? "GEMINI" : "MISTRAL";
    updateProfile.mutate({ aiProvider: next });
  };

  const startNew = () => {
    setConversationId(undefined);
    setMessages([]);
  };

  return (
    <div className="flex h-dvh flex-col pb-24">
      <header className="glass sticky top-0 z-10 flex items-center justify-between px-5 py-4">
        <div>
          <p className="lapidary text-[0.6rem] tracking-[0.25em] text-gold">L'Oracle</p>
          <h1 className="text-2xl text-marble">Conseil divin</h1>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={toggleProvider}
            className="lapidary bg-surface-high px-3 py-1.5 text-[0.6rem] tracking-[0.1em] text-gold"
          >
            {profile.data?.aiProvider ?? "MISTRAL"}
          </button>
          <button onClick={startNew} className="text-marble-dim hover:text-marble" aria-label="Nouvelle conversation">
            <IconSparkle size={20} />
          </button>
        </div>
      </header>

      <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4">
        {messages.length === 0 && (
          <div className="grid h-full place-items-center text-center">
            <div>
              <p className="lapidary text-sm tracking-[0.1em] text-marble-dim">
                Interroge l'Oracle
              </p>
              <p className="mt-2 max-w-xs text-xs text-marble-dim/70">
                « Quels sont mes macros aujourd'hui ? » · « J'ai mangé un burger, ajoute-le. »
              </p>
            </div>
          </div>
        )}
        {messages.map((m) => (
          <div
            key={m.id}
            className={cn("flex", m.role === "USER" ? "justify-end" : "justify-start")}
          >
            <div
              className={cn(
                "max-w-[82%] px-4 py-3 text-sm",
                m.role === "USER"
                  ? "gold-sheen text-[var(--color-on-gold)]"
                  : "bg-surface-high text-marble",
              )}
            >
              {m.pending ? <Spinner className="size-4" /> : m.content}
            </div>
          </div>
        ))}
        <div ref={endRef} />
      </div>

      <div className="glass px-4 py-3">
        {image && (
          <div className="mb-2 flex items-center gap-2 text-xs text-marble-dim">
            <span className="truncate">📷 {image.name}</span>
            <button onClick={() => setImage(null)} className="text-[var(--color-danger)]">
              <IconClose size={14} />
            </button>
          </div>
        )}
        <div className="flex items-end gap-2">
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            capture="environment"
            className="hidden"
            onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          />
          <button onClick={() => fileRef.current?.click()} className="p-2 text-marble-dim hover:text-gold" aria-label="Photo">
            <IconCamera size={22} />
          </button>
          {supported && (
            <button
              onClick={toggle}
              className={cn("p-2", listening ? "text-gold" : "text-marble-dim hover:text-gold")}
              aria-label="Dicter"
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
            placeholder="Parle à l'Oracle…"
            className="max-h-28 flex-1 resize-none bg-surface-lowest px-3 py-2.5 text-sm text-marble outline-none"
          />
          <button
            onClick={send}
            disabled={sending || (!input.trim() && !image)}
            className="gold-sheen p-2.5 text-[var(--color-on-gold)] disabled:opacity-40"
            aria-label="Envoyer"
          >
            <IconSend size={20} />
          </button>
        </div>
      </div>
    </div>
  );
}
