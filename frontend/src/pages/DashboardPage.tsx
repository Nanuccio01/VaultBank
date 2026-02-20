import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { clearToken } from "../auth/token";
import { latestTransfers, me, transfer } from "../api/bankingApi";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

const transferSchema = z.object({
    toIban: z
        .string()
        .regex(/^IT\d{2}[A-Z]\d{5}\d{5}\d{12}$/, "Invalid Italian IBAN"),
    amount: z.number().min(0.01, "Min 0.01"),
    causal: z.string().max(140, "Max 140 chars").optional(),
});

type TransferForm = z.infer<typeof transferSchema>;

type MovementItem = {
    id: string;
    direction: "IN" | "OUT";
    amount: string;
    causal?: string;
    createdAt: string;
    senderIban: string;
    recipientIban: string;
};

type MeData = {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    iban: string;
    balance: string;
};

export function DashboardPage() {
    const nav = useNavigate();

    const [loading, setLoading] = useState(true);
    const [meData, setMeData] = useState<MeData | null>(null);
    const [items, setItems] = useState<MovementItem[]>([]);

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<TransferForm>({
        resolver: zodResolver(transferSchema),
        defaultValues: { toIban: "", amount: 0.01, causal: "" },
    });

    async function load() {
        setLoading(true);
        try {
            const [m, t] = await Promise.all([me(), latestTransfers()]);
            setMeData(m as MeData);
            setItems(t as MovementItem[]);
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) {
                clearToken();
                nav("/login");
                return;
            }
            toast.error(e?.response?.data?.message || "Failed to load dashboard");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, []);

    const fullName = useMemo(() => {
        if (!meData) return "";
        return `${meData.firstName ?? ""} ${meData.lastName ?? ""}`.trim();
    }, [meData]);

    async function onTransfer(values: TransferForm) {
        try {
            const resp = await transfer({
                toIban: values.toIban,
                amount: values.amount,
                causal: values.causal ?? "",
            });

            toast.success(
                `Transfer executed. New balance: € ${formatMoney((resp as any).newBalance)}`
            );

            reset({ toIban: "", amount: 0.01, causal: "" });
            await load();
        } catch (e: any) {
            const data = e?.response?.data;
            const details = Array.isArray(data?.details) ? data.details.join(" | ") : "";
            toast.error(details ? `${data.message}: ${details}` : (data?.message || "Transfer failed"));
        }
    }

    function logout() {
        clearToken();
        nav("/login");
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-950 to-slate-900 text-white p-6">            <div className="max-w-5xl mx-auto">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold">VaultBank</h1>
                        <p className="text-slate-300 text-sm">Dashboard</p>
                    </div>
                    <button
                        onClick={logout}
                        className="rounded-xl border border-white/15 px-4 py-2 hover:bg-white/5"
                    >
                        Logout
                    </button>
                </div>

                {loading ? (
                    <div className="mt-8 text-slate-300">Loading...</div>
                ) : (
                    <>
                        <div className="grid md:grid-cols-3 gap-3 mt-5">
                            <Card title="Account holder" accent="blue">
                                <div className="text-base font-semibold leading-tight">{fullName || "—"}</div>
                                <div className="text-slate-300 text-xs mt-1">{meData?.email}</div>
                                <div className="text-slate-300 text-xs">{meData?.phone}</div>
                            </Card>

                            <Card title="IBAN" accent="slate">
                                <div className="flex items-start justify-between gap-3">
                                    <div className="min-w-0">
                                        <div className="font-mono text-sm break-all">{formatIban(meData?.iban)}</div>
                                        <div className="mt-2">
                                      <span className="text-[11px] px-2 py-1 rounded-full border border-white/15 text-slate-200 bg-white/5">
                                        IBAN ready
                                      </span>
                                        </div>
                                    </div>

                                    <button
                                        onClick={() => copyText(formatIban(meData?.iban))}
                                        disabled={!meData?.iban}
                                        className="rounded-xl border border-white/15 px-3 py-2 text-sm hover:bg-white/5 disabled:opacity-50"
                                    >
                                        Copy
                                    </button>
                                </div>
                            </Card>

                            <Card title="Balance" accent="emerald">
                                <div className="text-xl font-semibold">€ {formatMoney(meData?.balance)}</div>
                                <div className="text-slate-200/80 text-xs mt-1">
                                    Available balance
                                </div>
                            </Card>
                        </div>
                        <div className="grid md:grid-cols-2 gap-4 mt-6">
                            <Card title="New transfer">
                                <form className="space-y-3" onSubmit={handleSubmit(onTransfer)}>
                                    <div>
                                        <label className="text-sm text-slate-200">To IBAN</label>
                                        <input
                                            className="input mt-1"
                                            placeholder="IT..."
                                            {...register("toIban")}
                                            onChange={(e) => {
                                                e.target.value = e.target.value.replace(/\s+/g, "");
                                                register("toIban").onChange(e);
                                            }}
                                        />
                                    </div>

                                    <div>
                                        <label className="text-sm text-slate-200">Amount</label>
                                        <input
                                            className="input mt-1"
                                            placeholder="10.50"
                                            {...register("amount", { valueAsNumber: true })}
                                        />
                                        {errors.amount?.message && (
                                            <div className="text-xs text-red-300 mt-1">
                                                {errors.amount.message}
                                            </div>
                                        )}
                                    </div>

                                    <div>
                                        <label className="text-sm text-slate-200">
                                            Causale <span className="text-slate-400">(optional)</span>
                                        </label>
                                        <input
                                            className="input mt-1"
                                            placeholder="Es. Affitto Febbraio"
                                            {...register("causal")}
                                        />
                                        {errors.causal?.message && (
                                            <div className="text-xs text-red-300 mt-1">
                                                {errors.causal.message}
                                            </div>
                                        )}
                                    </div>

                                    <button
                                        disabled={isSubmitting}
                                        className="w-full rounded-xl bg-white text-slate-900 font-semibold py-2.5 hover:bg-slate-100 disabled:opacity-60"
                                    >
                                        {isSubmitting ? "Sending..." : "Send transfer"}
                                    </button>
                                </form>
                            </Card>

                            <Card title="Last Transfer">
                                <div className="space-y-3">
                                    {items?.length ? (
                                        items.map((m) => {
                                            const isIn = m.direction === "IN";
                                            const sign = isIn ? "+" : "-";
                                            const label = isIn ? "IN" : "OUT";
                                            const counterpartLine = isIn
                                                ? `From: ${formatIban(m.senderIban)}`
                                                : `To: ${formatIban(m.recipientIban)}`;

                                            return (
                                                <div
                                                    key={m.id}
                                                    className={
                                                        "rounded-2xl border p-4 " +
                                                        (isIn
                                                            ? "border-emerald-400/20 bg-emerald-400/5"
                                                            : "border-rose-400/20 bg-rose-400/5")
                                                    }
                                                >
                                                    <div className="flex items-start justify-between gap-3">
                                                        <div className="min-w-0">
                                                            <div className="flex items-center gap-2">
                                <span
                                    className={
                                        "text-[11px] px-2 py-1 rounded-full border " +
                                        (isIn
                                            ? "border-emerald-400/30 text-emerald-200 bg-emerald-400/10"
                                            : "border-rose-400/30 text-rose-200 bg-rose-400/10")
                                    }
                                >
                                  {label}
                                </span>
                                                                <div className="text-sm font-semibold truncate">
                                                                    {m.causal && String(m.causal).trim().length
                                                                        ? m.causal
                                                                        : "Bonifico"}
                                                                </div>
                                                            </div>

                                                            <div className="text-xs text-slate-300 mt-2 font-mono break-all">
                                                                {counterpartLine}
                                                            </div>

                                                            <div className="text-xs text-slate-400 mt-2">
                                                                {new Date(m.createdAt).toLocaleString()}
                                                            </div>
                                                        </div>

                                                        <div
                                                            className={
                                                                "whitespace-nowrap text-lg font-semibold " +
                                                                (isIn ? "text-emerald-200" : "text-rose-200")
                                                            }
                                                        >
                                                            {sign} € {formatMoney(m.amount)}
                                                        </div>
                                                    </div>
                                                </div>
                                            );
                                        })
                                    ) : (
                                        <div className="text-slate-300 text-sm">Nessun movimento.</div>
                                    )}
                                </div>
                            </Card>
                        </div>
                    </>
                )}
            </div>

            <style>
                {`
          .input {
            width: 100%;
            border-radius: 0.75rem;
            padding: 0.6rem 0.75rem;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.12);
            color: white;
            outline: none;
          }
          .input:focus { border-color: rgba(255,255,255,0.35); }
        `}
            </style>
        </div>
    );
}

function Card({
                  title,
                  children,
                  accent = "slate",
              }: {
    title: string;
    children: React.ReactNode;
    accent?: "slate" | "emerald" | "rose" | "blue";
}) {
    const accentMap: Record<string, string> = {
        slate: "from-white/10 to-white/5 border-white/10",
        emerald: "from-emerald-400/10 to-white/5 border-emerald-400/20",
        rose: "from-rose-400/10 to-white/5 border-rose-400/20",
        blue: "from-sky-400/10 to-white/5 border-sky-400/20",
    };

    return (
        <div
            className={
                "rounded-2xl border bg-gradient-to-br p-4 " + (accentMap[accent] ?? accentMap.slate)
            }
        >
            <div className="text-slate-300 text-xs">{title}</div>
            <div className="mt-2">{children}</div>
        </div>
    );
}

function formatMoney(v: any) {
    if (v === null || v === undefined || v === "") return "0.00";
    const n = Number(v);
    if (Number.isNaN(n)) return String(v);
    return n.toFixed(2);
}

function formatIban(iban?: string) {
    if (!iban) return "—";
    return iban.replace(/\s+/g, ""); // no spaces, always raw
}

async function copyText(text: string) {
    if (!text) return;
    try {
        await navigator.clipboard.writeText(text);
        toast.success("IBAN copiato");
    } catch {
        toast.error("Copia non riuscita");
    }
}