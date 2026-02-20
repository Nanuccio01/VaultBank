import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import toast from "react-hot-toast";
import { register as registerApi } from "../api/authApi";
import { Link, useNavigate } from "react-router-dom";

const schema = z.object({
    firstName: z.string().min(1).max(100),
    lastName: z.string().min(1).max(100),
    email: z.string().email().max(200),
    phone: z.string().min(7).max(20).regex(/^[0-9+ ]+$/),
    password: z.string().min(8).max(200),
});

type FormData = z.infer<typeof schema>;

export function RegisterPage() {
    const nav = useNavigate();
    const { register, handleSubmit, formState: { errors, isSubmitting } } =
        useForm<FormData>({ resolver: zodResolver(schema) });

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

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 flex items-center justify-center p-6">
            <div className="w-full max-w-md bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
                <h1 className="text-2xl font-semibold text-white">Create VaultBank account</h1>
                <p className="text-slate-300 mt-1 text-sm">Demo registration (PII encrypted at rest).</p>

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

                    <Field label="Password" error={errors.password?.message}>
                        <input className="input" type="password" {...register("password")} />
                    </Field>

                    <button disabled={isSubmitting} className="w-full rounded-xl bg-white text-slate-900 font-semibold py-2.5 hover:bg-slate-100 disabled:opacity-60">
                        {isSubmitting ? "Creating..." : "Register"}
                    </button>
                </form>

                <p className="text-sm text-slate-300 mt-4">
                    Already have an account? <Link className="text-white underline" to="/login">Login</Link>
                </p>
            </div>

            <StyleHelpers />
        </div>
    );
}

function Field({ label, error, children }: { label: string; error?: string; children: any }) {
    return (
        <div>
            <label className="text-sm text-slate-200">{label}</label>
            <div className="mt-1">{children}</div>
            {error && <div className="text-xs text-red-300 mt-1">{error}</div>}
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