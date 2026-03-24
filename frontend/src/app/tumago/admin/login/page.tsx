"use client";

import { useState, type FormEvent } from "react";
import { useAuth } from "@/lib/auth";
import Image from "next/image";

export default function AdminLoginPage() {
  const { login, isAuthenticated, isLoading } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // If the user is already authenticated, redirect immediately.
  // (The dashboard layout handles the actual redirect, but we can short-
  // circuit here to avoid a flash of the login form.)
  if (!isLoading && isAuthenticated) {
    // Use window.location for a hard redirect so Next.js re-evaluates
    // layouts from scratch.
    if (typeof window !== "undefined") {
      window.location.href = "/tumago/admin";
    }
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await login(email, password);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Login failed. Please try again.";
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-dark via-slate-900 to-slate-950 px-4">
      {/* Login card */}
      <div className="w-full max-w-md">
        {/* Brand header */}
        <div className="text-center mb-8">
          <Image
            src="/tuma_go_logo.png"
            alt="TumaGo"
            width={64}
            height={64}
            className="mx-auto mb-4"
          />
          <h1 className="text-2xl font-bold text-white">TumaGo Admin</h1>
          <p className="text-slate-400 mt-1 text-sm">
            Sign in to access the admin dashboard
          </p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          {/* Error banner */}
          {error && (
            <div className="mb-6 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Email */}
            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-text-dark mb-1.5"
              >
                Email address
              </label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@tumago.co.zw"
                className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm text-text-dark placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition"
              />
            </div>

            {/* Password */}
            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-text-dark mb-1.5"
              >
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm text-text-dark placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition"
              />
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-lg bg-primary py-2.5 text-sm font-semibold text-white hover:bg-primary-dark focus:ring-2 focus:ring-primary/40 focus:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed transition cursor-pointer"
            >
              {submitting ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-slate-500 mt-6">
          Protected area. Unauthorized access is prohibited.
        </p>
        <p className="text-center text-xs text-slate-500 mt-2">
          A product of{" "}
          <a
            href="https://tishanyq.co.zw"
            target="_blank"
            rel="noopener noreferrer"
            className="text-slate-400 hover:text-white transition-colors underline underline-offset-2"
          >
            Tishanyq Digital Pvt Ltd
          </a>
        </p>
      </div>
    </div>
  );
}
