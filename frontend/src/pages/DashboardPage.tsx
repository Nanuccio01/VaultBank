import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { clearToken } from "../auth/token";
import { latestTransfers, me, transfer } from "../api/bankingApi";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

const transferSchema = z.object({
    toIban: z.string().regex(/^IT\d{2}[A-Z]\d{5}\d{5}\d{12}$/, "Invalid Italian IBAN"),
    amount: z.number().min(0.01, "Min 0.01"),
});
type TransferForm = z.infer<typeof transferSchema>;

export function DashboardPage() {
    const nav = useNavigate();
    const [loading, setLoading] = useState(true);
    const [meData, setMeData] = useState<any>(null);
    const [items, setItems] = useState<any[]>([]);

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<TransferForm>({
        resolver: zodResolver(transferSchema),
        defaultValues: { toIban: "", amount: 0.01 },
    });

    async function load() {
        setLoading(true);
        try {
            const [m, t] = await Promise.all([me(), latestTransfers()]);
            setMeData(m);
            setItems(t);
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

    useEffect(() => { load(); }, []);

    const fullName = useMemo(() => {
        if (!meData) return "";
        return `${meData.firstName ?? ""} ${meData.lastName ?? ""}`.trim();
    }, [meData]);

    async function onTransfer(values: TransferForm) {
        try {
            const resp = await transfer({ toIban: values.toIban, amount: values.amount });
            toast.success(`Transfer executed. New balance: € ${formatMoney(resp.newBalance)}`);
            reset(); // torna ai defaultValues
            await load();
        } catch (e: any) {
            const msg = e?.response?.data?.message || "Transfer failed";
            toast.error(msg);
        }
    }

    function logout() {
        clearToken();
        nav("/login");
    }

    return (
        <div className="min-h-screen bg-slate-950 text-white p-6">
            <div className="max-w-5xl mx-auto">
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold">Dashboard</h1>
                        <p className="text-slate-300 text-sm">VaultBank demo • JWT + AES at rest</p>
                    </div>
                    <button onClick={logout} className="rounded-xl border border-white/15 px-4 py-2 hover:bg-white/5">
                        Logout
                    </button>
                </div>

                {loading ? (
                    <div className="mt-8 text-slate-300">Loading...</div>
                ) : (
                    <>
                        <div className="grid md:grid-cols-3 gap-4 mt-6">
                            <Card title="Account holder">
                                <div className="text-lg font-semibold">{fullName || "—"}</div>
                                <div className="text-slate-300 text-sm">{meData?.email}</div>
                                <div className="text-slate-300 text-sm">{meData?.phone}</div>
                            </Card>

                            <Card title="IBAN">
                                <div className="font-mono text-sm break-all">{meData?.iban}</div>
                            </Card>

                            <Card title="Balance">
                                <div className="text-2xl font-semibold">
                                    € {formatMoney(meData?.balance)}
                                </div>
                                <div className="text-slate-400 text-xs mt-1">Stored encrypted in DB</div>
                            </Card>
                        </div>

                        <div className="grid md:grid-cols-2 gap-4 mt-6">
                            <Card title="New transfer">
                                <form className="space-y-3" onSubmit={handleSubmit(onTransfer)}>
                                    <div>
                                        <label className="text-sm text-slate-200">To IBAN</label>
                                        <input className="input mt-1" placeholder="IT.." {...register("toIban")} />
                                        {errors.toIban?.message && <div className="text-xs text-red-300 mt-1">{errors.toIban.message}</div>}
                                    </div>
                                    <div>
                                        <label className="text-sm text-slate-200">Amount</label>
                                        <input className="input mt-1" placeholder="10.50" {...register("amount", { valueAsNumber: true })} />
                                        {errors.amount?.message && <div className="text-xs text-red-300 mt-1">{errors.amount.message}</div>}
                                    </div>
                                    <button disabled={isSubmitting} className="w-full rounded-xl bg-white text-slate-900 font-semibold py-2.5 hover:bg-slate-100 disabled:opacity-60">
                                        {isSubmitting ? "Sending..." : "Send transfer"}
                                    </button>
                                </form>
                            </Card>

                            <Card title="Latest transfers">
                                <div className="space-y-3">
                                    {items?.length ? items.map((t) => (
                                        <div key={t.id} className="rounded-xl border border-white/10 p-3">
                                            <div className="flex justify-between gap-3">
                                                <div className="font-mono text-xs break-all text-slate-200">{t.toIban}</div>
                                                <div className="whitespace-nowrap font-semibold">€ {formatMoney(t.amount)}</div>
                                            </div>
                                            <div className="text-slate-400 text-xs mt-1">{new Date(t.createdAt).toLocaleString()}</div>
                                        </div>
                                    )) : (
                                        <div className="text-slate-300 text-sm">No transfers yet.</div>
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

function Card({ title, children }: { title: string; children: any }) {
    return (
        <div className="rounded-2xl bg-white/5 border border-white/10 p-5">
            <div className="text-slate-300 text-sm">{title}</div>
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