"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  Eye,
  EyeOff,
  Truck,
  Loader2,
} from "lucide-react";

const countryCodes = [
  { code: "+263", label: "ZW +263", country: "Zimbabwe" },
  { code: "+27", label: "ZA +27", country: "South Africa" },
  { code: "+258", label: "MZ +258", country: "Mozambique" },
  { code: "+260", label: "ZM +260", country: "Zambia" },
  { code: "+267", label: "BW +267", country: "Botswana" },
];

export default function DriverSignupPage() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    surname: "",
    email: "",
    countryCode: "+263",
    phone: "",
    streetAdress: "",
    addressLine: "",
    city: "",
    province: "",
    postalCode: "",
    password: "",
    confirmPassword: "",
  });

  function updateField(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setError("");
  }

  function validateStep1(): boolean {
    if (!form.name.trim()) {
      setError("Please enter your first name");
      return false;
    }
    if (!form.surname.trim()) {
      setError("Please enter your surname");
      return false;
    }
    if (!form.email.trim()) {
      setError("Please enter your email address");
      return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError("Please enter a valid email address");
      return false;
    }
    if (!form.phone.trim()) {
      setError("Please enter your phone number");
      return false;
    }
    if (!/^\d{7,15}$/.test(form.phone.replace(/^0/, ""))) {
      setError("Please enter a valid phone number");
      return false;
    }
    return true;
  }

  function validateStep2(): boolean {
    if (!form.streetAdress.trim()) {
      setError("Please enter your street address");
      return false;
    }
    if (!form.city.trim()) {
      setError("Please enter your city");
      return false;
    }
    if (!form.province.trim()) {
      setError("Please enter your province");
      return false;
    }
    if (!form.postalCode.trim()) {
      setError("Please enter your postal code");
      return false;
    }
    return true;
  }

  function validateStep3(): boolean {
    if (form.password.length < 6) {
      setError("Password must be at least 6 characters");
      return false;
    }
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match");
      return false;
    }
    return true;
  }

  function nextStep() {
    if (step === 1 && validateStep1()) {
      setStep(2);
      setError("");
    } else if (step === 2 && validateStep2()) {
      setStep(3);
      setError("");
    }
  }

  function prevStep() {
    setStep((s) => Math.max(1, s - 1));
    setError("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validateStep3()) return;

    setLoading(true);
    setError("");

    const rawPhone = form.phone.replace(/^0/, "");
    const fullPhone = form.countryCode + rawPhone;

    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || ""}/api/driver/signup/`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: form.name.trim(),
            surname: form.surname.trim(),
            email: form.email.trim().toLowerCase(),
            phone_number: fullPhone,
            password: form.password,
            streetAdress: form.streetAdress.trim(),
            addressLine: form.addressLine.trim(),
            city: form.city.trim(),
            province: form.province.trim(),
            postalCode: form.postalCode.trim(),
            verifiedEmail: true,
          }),
        }
      );

      if (res.ok) {
        router.push("/get-started/welcome?role=driver");
      } else {
        const data = await res.json().catch(() => null);
        const msg =
          data?.detail ||
          data?.email?.[0] ||
          data?.phone_number?.[0] ||
          "Registration failed. Please try again.";
        setError(msg);
      }
    } catch {
      // If API is unavailable (pre-launch), still redirect to welcome
      router.push("/get-started/welcome?role=driver");
    } finally {
      setLoading(false);
    }
  }

  const stepLabels = ["Personal info", "Address", "Create password"];

  return (
    <main className="min-h-screen bg-gradient-to-br from-primary-light via-white to-white pt-28 pb-20 lg:pt-36 lg:pb-28">
      <div className="mx-auto max-w-lg px-4 sm:px-6">
        {/* Back link */}
        <Link
          href="/get-started"
          className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-text-muted hover:text-primary transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Choose a different role
        </Link>

        {/* Card */}
        <div className="rounded-2xl border border-gray-100 bg-white p-8 shadow-lg sm:p-10">
          {/* Header */}
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Truck className="h-7 w-7" />
            </div>
            <h1 className="text-2xl font-bold text-text-dark">
              Create Driver Account
            </h1>
            <p className="mt-2 text-sm text-text-muted">
              Step {step} of 3 — {stepLabels[step - 1]}
            </p>
          </div>

          {/* Step indicator */}
          <div className="mb-8 flex gap-2">
            {[1, 2, 3].map((s) => (
              <div
                key={s}
                className={`h-1.5 flex-1 rounded-full transition-colors ${
                  s <= step ? "bg-primary" : "bg-gray-200"
                }`}
              />
            ))}
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Step 1: Personal Info */}
            {step === 1 && (
              <div className="space-y-5">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    First Name
                  </label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => updateField("name", e.target.value)}
                    placeholder="Tatenda"
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Surname
                  </label>
                  <input
                    type="text"
                    value={form.surname}
                    onChange={(e) => updateField("surname", e.target.value)}
                    placeholder="Moyo"
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => updateField("email", e.target.value)}
                    placeholder="tatenda@example.com"
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Phone Number
                  </label>
                  <div className="flex gap-2">
                    <select
                      value={form.countryCode}
                      onChange={(e) =>
                        updateField("countryCode", e.target.value)
                      }
                      className="w-28 rounded-xl border border-gray-200 px-3 py-3 text-sm text-text-dark outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    >
                      {countryCodes.map((cc) => (
                        <option key={cc.code} value={cc.code}>
                          {cc.label}
                        </option>
                      ))}
                    </select>
                    <input
                      type="tel"
                      value={form.phone}
                      onChange={(e) => updateField("phone", e.target.value)}
                      placeholder="77 123 4567"
                      className="flex-1 rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    />
                  </div>
                </div>
                <button
                  type="button"
                  onClick={nextStep}
                  className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3.5 text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg"
                >
                  Continue
                  <ArrowRight className="h-4 w-4" />
                </button>
              </div>
            )}

            {/* Step 2: Address */}
            {step === 2 && (
              <div className="space-y-5">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Street Address
                  </label>
                  <input
                    type="text"
                    value={form.streetAdress}
                    onChange={(e) => updateField("streetAdress", e.target.value)}
                    placeholder="123 Samora Machel Ave"
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Address Line 2{" "}
                    <span className="text-text-muted font-normal">
                      (optional)
                    </span>
                  </label>
                  <input
                    type="text"
                    value={form.addressLine}
                    onChange={(e) => updateField("addressLine", e.target.value)}
                    placeholder="Apt, suite, floor, etc."
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-text-dark">
                      City
                    </label>
                    <input
                      type="text"
                      value={form.city}
                      onChange={(e) => updateField("city", e.target.value)}
                      placeholder="Harare"
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    />
                  </div>
                  <div>
                    <label className="mb-1.5 block text-sm font-medium text-text-dark">
                      Province
                    </label>
                    <input
                      type="text"
                      value={form.province}
                      onChange={(e) => updateField("province", e.target.value)}
                      placeholder="Harare"
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    />
                  </div>
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Postal Code
                  </label>
                  <input
                    type="text"
                    value={form.postalCode}
                    onChange={(e) => updateField("postalCode", e.target.value)}
                    placeholder="00263"
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                  />
                </div>
                <div className="mt-2 flex gap-3">
                  <button
                    type="button"
                    onClick={prevStep}
                    className="flex items-center justify-center gap-2 rounded-xl border-2 border-gray-200 px-5 py-3.5 text-sm font-semibold text-text-dark transition-all hover:border-gray-300 hover:bg-gray-50"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back
                  </button>
                  <button
                    type="button"
                    onClick={nextStep}
                    className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3.5 text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg"
                  >
                    Continue
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Password */}
            {step === 3 && (
              <div className="space-y-5">
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      value={form.password}
                      onChange={(e) => updateField("password", e.target.value)}
                      placeholder="At least 6 characters"
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 pr-11 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-text-dark"
                    >
                      {showPassword ? (
                        <EyeOff className="h-5 w-5" />
                      ) : (
                        <Eye className="h-5 w-5" />
                      )}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="mb-1.5 block text-sm font-medium text-text-dark">
                    Confirm Password
                  </label>
                  <div className="relative">
                    <input
                      type={showConfirm ? "text" : "password"}
                      value={form.confirmPassword}
                      onChange={(e) =>
                        updateField("confirmPassword", e.target.value)
                      }
                      placeholder="Re-enter your password"
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 pr-11 text-sm text-text-dark placeholder:text-gray-400 outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm(!showConfirm)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-text-dark"
                    >
                      {showConfirm ? (
                        <EyeOff className="h-5 w-5" />
                      ) : (
                        <Eye className="h-5 w-5" />
                      )}
                    </button>
                  </div>
                </div>
                <div className="mt-2 flex gap-3">
                  <button
                    type="button"
                    onClick={prevStep}
                    className="flex items-center justify-center gap-2 rounded-xl border-2 border-gray-200 px-5 py-3.5 text-sm font-semibold text-text-dark transition-all hover:border-gray-300 hover:bg-gray-50"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3.5 text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      "Create Account"
                    )}
                  </button>
                </div>
              </div>
            )}
          </form>
        </div>
      </div>
    </main>
  );
}
