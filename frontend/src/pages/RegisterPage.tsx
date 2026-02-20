import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import toast from "react-hot-toast";
import { register as registerApi } from "../api/authApi";
import { Link, useNavigate } from "react-router-dom";

const passwordRules = [
    { id: "len", label: "At least 10 characters", test: (p: string) => p.length >= 10 },
    { id: "lower", label: "At least 1 lowercase letter (a-z)", test: (p: string) => /[a-z]/.test(p) },
    { id: "upper", label: "At least 1 uppercase letter (A-Z)", test: (p: string) => /[A-Z]/.test(p) },
    { id: "digit", label: "At least 1 number (0-9)", test: (p: string) => /\d/.test(p) },
    { id: "symbol", label: "At least 1 symbol (!@#$...)", test: (p: string) => /[^A-Za-z0-9\s]/.test(p) },
    { id: "nospace", label: "No spaces", test: (p: string) => !/\s/.test(p) },
] as const;

function passwordChecklist(password: string) {
    const p = password ?? "";
    return passwordRules.map(r => ({ ...r, ok: r.test(p) }));
}

function passwordScore(password: string) {
    const checks = passwordChecklist(password);
    const passed = checks.filter(c => c.ok).length;
    const total = checks.length;
    const pct = Math.round((passed / total) * 100);

    let label: "Very weak" | "Weak" | "Fair" | "Good" | "Strong";
    if (passed <= 1) label = "Very weak";
    else if (passed === 2) label = "Weak";
    else if (passed === 3) label = "Fair";
    else if (passed === 4) label = "Good";
    else label = "Strong";

    return { passed, total, pct, label, checks };
}

const schema = z.object({
    firstName: z.string().min(1, "First name is required").max(100, "Max 100 chars"),
    lastName: z.string().min(1, "Last name is required").max(100, "Max 100 chars"),
    email: z.string().email("Invalid email").max(200, "Max 200 chars"),
    phone: z
        .string()
        .min(7, "Phone too short")
        .max(20, "Phone too long")
        .regex(/^[0-9+ ]+$/, "Phone must contain digits and optional +/spaces"),
    password: z.string().superRefine((val, ctx) => {
        const checks = passwordChecklist(val);
        for (const c of checks) {
            if (!c.ok) {
                ctx.addIssue({
                    code: z.ZodIssueCode.custom,
                    message: c.label,
                });
            }
        }
    }),
});

type FormData = z.infer<typeof schema>;

export function RegisterPage() {
    const nav = useNavigate();

    const {
        register,
        handleSubmit,
        watch,
        formState: { errors, isSubmitting, isSubmitted },
    } = useForm<FormData>({
        resolver: zodResolver(schema),
        criteriaMode: "all", // collect all password issues
        defaultValues: {
            firstName: "",
            lastName: "",
            email: "",
            phone: "",
            password: "",
        },
    });

    const passwordValue = watch("password") ?? "";
    const strength = useMemo(() => passwordScore(passwordValue), [passwordValue]);

    async function onSubmit(values: FormData) {
        try {
            await registerApi(values);
            toast.success("Account created. Please login.");
            nav("/login");
        } catch (e: any) {
            const msg = e?.response?.data?.message || "Registration failed";
            toast.error(msg);
        }
    }

    const showPasswordHints = passwordValue.length > 0 || isSubmitted;

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 flex items-center justify-center p-6">
            <div className="w-full max-w-md bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
                <h1 className="text-2xl font-semibold text-white">Create VaultBank account</h1>
                <p className="text-slate-300 mt-1 text-sm">Demo registration</p>

                <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
                    <Field label="First name" error={errors.firstName?.message}>
                        <input className="input" {...register("firstName")} />
                    </Field>

                    <Field label="Last name" error={errors.lastName?.message}>
                        <input className="input" {...register("lastName")} />
                    </Field>

                    <Field label="Email" error={errors.email?.message}>
                        <input className="input" type="email" {...register("email")} />
                    </Field>

                    <Field label="Phone" error={errors.phone?.message}>
                        <input className="input" placeholder="+39 333 1234567" {...register("phone")} />
                    </Field>

                    <div>
                        <label className="text-sm text-slate-200">Password</label>
                        <div className="mt-1">
                            <input className="input" type="password" {...register("password")} />
                        </div>

                        <PasswordStrengthBar pct={strength.pct} label={strength.label} />

                        {showPasswordHints && (
                            <div className="mt-3 space-y-1">
                                {strength.checks.map((c) => (
                                    <div
                                        key={c.id}
                                        className={"text-xs " + (c.ok ? "text-emerald-200" : "text-rose-200")}
                                    >
                                        {c.ok ? "✓ " : "✗ "}
                                        {c.label}
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* If user submits and password invalid, show a single compact line too */}
                        {isSubmitted && errors.password && (
                            <div className="text-xs text-rose-200 mt-2">
                                Password does not meet the requirements above.
                            </div>
                        )}
                    </div>

                    <button
                        disabled={isSubmitting}
                        className="w-full rounded-xl bg-white text-slate-900 font-semibold py-2.5 hover:bg-slate-100 disabled:opacity-60"
                    >
                        {isSubmitting ? "Creating..." : "Register"}
                    </button>
                </form>

                <p className="text-sm text-slate-300 mt-4">
                    Already have an account?{" "}
                    <Link className="text-white underline" to="/login">
                        Login
                    </Link>
                </p>
            </div>

            <StyleHelpers />
        </div>
    );
}

function PasswordStrengthBar({ pct, label }: { pct: number; label: string }) {
    // color mapping
    const color =
        pct <= 20 ? "bg-rose-400" :
            pct <= 40 ? "bg-orange-400" :
                pct <= 60 ? "bg-amber-300" :
                    pct <= 80 ? "bg-emerald-300" :
                        "bg-emerald-400";

    return (
        <div className="mt-2">
            <div className="flex items-center justify-between">
                <div className="text-xs text-slate-300">Password strength</div>
                <div className="text-xs text-slate-200">{label}</div>
            </div>
            <div className="mt-1 h-2 w-full rounded-full bg-white/10 overflow-hidden">
                <div className={`h-full ${color}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

function Field({
                   label,
                   error,
                   children,
               }: {
    label: string;
    error?: string;
    children: React.ReactNode;
}) {
    return (
        <div>
            <label className="text-sm text-slate-200">{label}</label>
            <div className="mt-1">{children}</div>
            {error && <div className="text-xs text-rose-200 mt-1">{error}</div>}
        </div>
    );
}

function StyleHelpers() {
    return (
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
        .input:focus {
          border-color: rgba(255,255,255,0.35);
        }
      `}
        </style>
    );
}