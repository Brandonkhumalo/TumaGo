"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { CheckCircle, Mail, ArrowRight, Package } from "lucide-react";

const LAUNCH_DATE = new Date("2026-05-01T00:00:00").getTime();

function getTimeLeft() {
  const now = Date.now();
  const diff = Math.max(0, LAUNCH_DATE - now);

  return {
    days: Math.floor(diff / (1000 * 60 * 60 * 24)),
    hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
    minutes: Math.floor((diff / (1000 * 60)) % 60),
    seconds: Math.floor((diff / 1000) % 60),
  };
}

function CountdownTimer() {
  const [time, setTime] = useState(getTimeLeft);

  useEffect(() => {
    const interval = setInterval(() => {
      setTime(getTimeLeft());
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const units = [
    { label: "Days", value: time.days },
    { label: "Hours", value: time.hours },
    { label: "Minutes", value: time.minutes },
    { label: "Seconds", value: time.seconds },
  ];

  return (
    <div className="flex justify-center gap-3 sm:gap-5">
      {units.map((unit) => (
        <div key={unit.label} className="text-center">
          <div className="flex h-18 w-18 sm:h-22 sm:w-22 items-center justify-center rounded-2xl bg-white shadow-lg border border-gray-100">
            <span className="text-2xl sm:text-4xl font-extrabold text-primary-dark tabular-nums">
              {String(unit.value).padStart(2, "0")}
            </span>
          </div>
          <p className="mt-2 text-xs sm:text-sm font-medium text-text-muted">
            {unit.label}
          </p>
        </div>
      ))}
    </div>
  );
}

function WelcomeContent() {
  const searchParams = useSearchParams();
  const role = searchParams.get("role") || "sender";
  const isDriver = role === "driver";

  return (
    <>
      {/* Success Card */}
      <div className="rounded-2xl border border-gray-100 bg-white p-8 shadow-lg sm:p-12 text-center">
        {/* Success Icon */}
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-green-50">
          <CheckCircle className="h-10 w-10 text-green-500" />
        </div>

        <h1 className="text-3xl font-extrabold text-text-dark sm:text-4xl">
          Thank You for Registering!
        </h1>

        <p className="mt-4 text-lg text-text-muted leading-relaxed max-w-md mx-auto">
          {isDriver
            ? "Welcome to the TumaGo driver network! We're excited to have you on board."
            : "Welcome to TumaGo! We're excited to have you as part of our community."}
        </p>

        {/* Email notice */}
        <div className="mt-8 rounded-xl bg-primary-light p-6">
          <div className="flex justify-center mb-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
              <Mail className="h-6 w-6 text-primary" />
            </div>
          </div>
          <h2 className="text-lg font-bold text-text-dark mb-2">
            Keep an Eye on Your Inbox
          </h2>
          <p className="text-sm text-text-muted leading-relaxed max-w-sm mx-auto">
            We&apos;ll send you email updates with more information on when
            the TumaGo app will be live on the Google Play Store. Stay tuned!
          </p>
        </div>

        {/* Countdown Section */}
        <div className="mt-10">
          <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-2">
            Launching In
          </p>
          <h3 className="text-xl font-bold text-text-dark mb-6">
            1 May 2026
          </h3>
          <CountdownTimer />
        </div>

        {/* Back to home */}
        <div className="mt-10">
          <Link
            href="/"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-7 py-3.5 text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg"
          >
            Back to Home
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </div>

      {/* See you soon */}
      <div className="mt-12 text-center">
        <div className="flex justify-center mb-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-white">
            <Package className="h-6 w-6" />
          </div>
        </div>
        <p className="text-2xl font-bold text-text-dark">
          See you soon{" "}
          <span className="text-primary">
            &#x2715; TumaGo
          </span>
        </p>
      </div>
    </>
  );
}

export default function WelcomePage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-primary-light via-white to-white pt-28 pb-20 lg:pt-36 lg:pb-28">
      <div className="mx-auto max-w-2xl px-4 sm:px-6 lg:px-8">
        <Suspense>
          <WelcomeContent />
        </Suspense>
      </div>
    </main>
  );
}
