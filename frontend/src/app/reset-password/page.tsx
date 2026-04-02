"use client";

import { useState, type FormEvent, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Image from "next/image";

const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:80/api/v1";

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!token) {
      setError("Invalid reset link. Please request a new one from the app.");
      return;
    }
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API_URL}/reset-password-confirm/`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token,
          password,
          confirm_password: confirmPassword,
        }),
      });

      const data = await res.json().catch(() => ({ detail: "Request failed" }));

      if (!res.ok) {
        setError(data.detail || "Failed to reset password. Please try again.");
        return;
      }

      setSuccess(true);
    } catch {
      setError("Connection error. Please check your internet and try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <div className="text-center">
        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Password Reset!</h2>
        <p className="text-gray-600 mb-6">
          Your password has been updated successfully. You can now log in with your new password in the TumaGo app.
        </p>
        <p className="text-sm text-gray-400">You can close this page.</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div>
        <h2 className="text-2xl font-bold text-gray-900 mb-1">Set New Password</h2>
        <p className="text-sm text-gray-500">Enter your new password below.</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div>
        <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
          New Password
        </label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="At least 6 characters"
          required
          minLength={6}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-gray-900 placeholder-gray-400"
        />
      </div>

      <div>
        <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
          Confirm Password
        </label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="Re-enter your password"
          required
          minLength={6}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-gray-900 placeholder-gray-400"
        />
      </div>

      <button
        type="submit"
        disabled={submitting}
        className="w-full py-3 px-4 bg-[#0e74bc] text-white font-semibold rounded-lg hover:bg-[#0a5a94] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {submitting ? "Resetting..." : "Reset Password"}
      </button>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#EFF8FE] px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Image
            src="/tuma_go_logo.png"
            alt="TumaGo"
            width={80}
            height={80}
            className="mx-auto mb-3"
          />
          <p className="text-gray-500 text-sm">Last-mile delivery, simplified</p>
        </div>

        <div className="bg-white rounded-2xl shadow-lg p-8">
          <Suspense fallback={<div className="text-center text-gray-500">Loading...</div>}>
            <ResetPasswordForm />
          </Suspense>
        </div>

        <p className="text-center text-xs text-gray-400 mt-6">
          &copy; TumaGo — Harare, Zimbabwe
        </p>
      </div>
    </div>
  );
}
