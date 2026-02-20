import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import toast from "react-hot-toast";
import { login as loginApi } from "../api/authApi";
import { setToken } from "../auth/token";
import { Link, useNavigate } from "react-router-dom";

const schema = z.object({
    email: z.string().email(),
    password: z.string().min(1),
});
type FormData = z.infer<typeof schema>;

export function LoginPage() {
    const nav = useNavigate();
    const { register, handleSubmit, formState: { errors, isSubmitting } } =
        useForm<FormData>({ resolver: zodResolver(schema) });

    async function onSubmit(values: FormData) {
        try {
            const resp = await loginApi(values);
            setToken(resp.accessToken);
            toast.success("Logged in");
            nav("/dashboard");
        } catch (e: any) {
            const status = e?.response?.status;
            const msg = status === 401 ? "Invalid credentials" : (e?.response?.data?.message || "Login failed");
            toast.error(msg);
        }
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 flex items-center justify-center p-6">
            <div className="w-full max-w-md bg-white/5 backdrop-blur rounded-2xl p-6 border border-white/10">
                <h1 className="text-2xl font-semibold text-white">VaultBank</h1>
                <p className="text-slate-300 mt-1 text-sm">Login to access your dashboard.</p>

                <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
                    <Field label="Email" error={errors.email?.message}>
                        <input className="input" type="email" {...register("email")} />
                    </Field>

                    <Field label="Password" error={errors.password?.message}>
                        <input className="input" type="password" {...register("password")} />
                    </Field>

                    <button disabled={isSubmitting} className="w-full rounded-xl bg-white text-slate-900 font-semibold py-2.5 hover:bg-slate-100 disabled:opacity-60">
                        {isSubmitting ? "Signing in..." : "Login"}
                    </button>
                </form>

                <p className="text-sm text-slate-300 mt-4">
                    No account? <Link className="text-white underline" to="/register">Register</Link>
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
        .input:focus { border-color: rgba(255,255,255,0.35); }
      `}
        </style>
    );
}