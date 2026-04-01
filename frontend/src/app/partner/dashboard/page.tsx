"use client";

import { useState, useEffect, useCallback, type FormEvent } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  Key,
  Smartphone,
  Plus,
  Eye,
  EyeOff,
  Copy,
  Check,
  LogOut,
  Loader2,
  ArrowRight,
  ShoppingCart,
  ToggleLeft,
  ToggleRight,
  Wallet,
  ArrowLeft,
} from "lucide-react";
import { partnerAPI } from "@/lib/api";

interface PartnerAccount {
  id: string;
  name: string;
  email: string;
  phone: string;
  api_key: string;
  api_secret: string;
  webhook_url: string;
  balance: number;
  commission_rate: number;
  max_device_slots: number;
  device_count: number;
  created_at: string;
  setup_fee_paid: boolean;
  is_suspended: boolean;
  is_permanently_banned: boolean;
}

interface Device {
  id: string;
  label: string;
  email: string;
  is_active: boolean;
  created_at: string;
}

type View = "login" | "dashboard";

export default function PartnerDashboard() {
  const [view, setView] = useState<View>("login");
  const [token, setToken] = useState("");
  const [account, setAccount] = useState<PartnerAccount | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Login form
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [showLoginPw, setShowLoginPw] = useState(false);

  // Create device form
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [deviceLabel, setDeviceLabel] = useState("");
  const [deviceEmail, setDeviceEmail] = useState("");
  const [devicePassword, setDevicePassword] = useState("");

  // Visibility toggles
  const [showApiKey, setShowApiKey] = useState(false);
  const [showApiSecret, setShowApiSecret] = useState(false);
  const [copiedField, setCopiedField] = useState("");

  // Check for stored token on mount
  useEffect(() => {
    const stored = sessionStorage.getItem("partner_token");
    if (stored) {
      setToken(stored);
      setView("dashboard");
    }
  }, []);

  const loadDashboard = useCallback(async (t: string) => {
    try {
      const [acct, devs] = await Promise.all([
        partnerAPI.account(t),
        partnerAPI.listDevices(t),
      ]);
      setAccount(acct);
      setDevices(devs.devices);
    } catch {
      sessionStorage.removeItem("partner_token");
      setToken("");
      setView("login");
      setError("Session expired. Please log in again.");
    }
  }, []);

  useEffect(() => {
    if (view === "dashboard" && token) {
      loadDashboard(token);
    }
  }, [view, token, loadDashboard]);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await partnerAPI.login(loginEmail, loginPassword);
      setToken(res.token);
      sessionStorage.setItem("partner_token", res.token);
      setView("dashboard");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Login failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    sessionStorage.removeItem("partner_token");
    setToken("");
    setAccount(null);
    setDevices([]);
    setView("login");
  };

  const handleCreateDevice = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      await partnerAPI.createDevice(token, {
        label: deviceLabel,
        email: deviceEmail,
        password: devicePassword,
      });
      setSuccess(`Device "${deviceLabel}" created successfully.`);
      setDeviceLabel("");
      setDeviceEmail("");
      setDevicePassword("");
      setShowCreateForm(false);
      await loadDashboard(token);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create device.");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleDevice = async (deviceId: string) => {
    setError("");
    try {
      await partnerAPI.toggleDevice(token, deviceId);
      await loadDashboard(token);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Failed to toggle device."
      );
    }
  };

  const handlePurchaseSlots = async () => {
    setError("");
    setSuccess("");
    setLoading(true);
    try {
      const res = await partnerAPI.purchaseDeviceSlots(token);
      setSuccess(res.message);
      await loadDashboard(token);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Failed to purchase slots."
      );
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = async (text: string, field: string) => {
    await navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(""), 2000);
  };

  const maskValue = (val: string) =>
    val.substring(0, 8) + "•".repeat(Math.max(0, val.length - 8));

  // ---------- LOGIN VIEW ----------
  if (view === "login") {
    return (
      <main className="min-h-screen bg-primary-light flex items-center justify-center px-4">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <Link href="/" className="inline-flex items-center gap-2 mb-6">
              <Image
                src="/tuma_go_logo.png"
                alt="TumaGo"
                width={40}
                height={40}
              />
              <span className="text-xl font-bold text-primary-dark">
                Tuma<span className="text-primary">Go</span>
              </span>
            </Link>
            <h1 className="text-2xl font-bold text-text-dark">
              Partner Dashboard
            </h1>
            <p className="text-sm text-text-muted mt-1">
              Log in to manage your business account
            </p>
          </div>

          <div className="rounded-2xl bg-white p-8 shadow-lg border border-gray-100">
            <form onSubmit={handleLogin} className="space-y-5">
              <div>
                <label
                  htmlFor="loginEmail"
                  className="block text-sm font-medium text-text-dark mb-1.5"
                >
                  Business Email
                </label>
                <input
                  id="loginEmail"
                  type="email"
                  required
                  value={loginEmail}
                  onChange={(e) => setLoginEmail(e.target.value)}
                  className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                  placeholder="admin@company.co.zw"
                />
              </div>
              <div>
                <label
                  htmlFor="loginPassword"
                  className="block text-sm font-medium text-text-dark mb-1.5"
                >
                  Password
                </label>
                <div className="relative">
                  <input
                    id="loginPassword"
                    type={showLoginPw ? "text" : "password"}
                    required
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    className="w-full rounded-xl border border-gray-200 px-4 py-3 pr-11 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                  />
                  <button
                    type="button"
                    onClick={() => setShowLoginPw(!showLoginPw)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showLoginPw ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>

              {error && (
                <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-xl bg-primary py-3.5 text-sm font-semibold text-white shadow-lg transition-all hover:bg-primary-dark hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Logging in...
                  </>
                ) : (
                  "Log In"
                )}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-sm text-text-muted">
                Don&apos;t have an account?{" "}
                <Link
                  href="/partner#register"
                  className="text-primary font-semibold hover:text-primary-dark"
                >
                  Register now
                </Link>
              </p>
            </div>
          </div>

          <div className="text-center mt-6">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-sm text-text-muted hover:text-text-dark transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to home
            </Link>
          </div>
        </div>
      </main>
    );
  }

  // ---------- DASHBOARD VIEW ----------
  return (
    <main className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8 flex items-center justify-between h-16">
          <div className="flex items-center gap-3">
            <Link href="/">
              <Image
                src="/tuma_go_logo.png"
                alt="TumaGo"
                width={32}
                height={32}
              />
            </Link>
            <div>
              <h1 className="text-sm font-bold text-text-dark">
                Partner Dashboard
              </h1>
              {account && (
                <p className="text-xs text-text-muted">{account.name}</p>
              )}
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 text-sm text-text-muted hover:text-red-600 transition-colors"
          >
            <LogOut className="h-4 w-4" />
            Log Out
          </button>
        </div>
      </header>

      <div className="mx-auto max-w-6xl px-4 sm:px-6 lg:px-8 py-8">
        {/* Alerts */}
        {error && (
          <div className="mb-6 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
            {error}
            <button
              onClick={() => setError("")}
              className="float-right font-bold"
            >
              &times;
            </button>
          </div>
        )}
        {success && (
          <div className="mb-6 rounded-xl bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
            {success}
            <button
              onClick={() => setSuccess("")}
              className="float-right font-bold"
            >
              &times;
            </button>
          </div>
        )}

        {!account ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : account.is_permanently_banned || account.is_suspended ? (
          <div className="rounded-xl border border-red-200 bg-red-50 p-8 text-center">
            <h2 className="text-xl font-bold text-red-700 mb-2">Account Suspended</h2>
            <p className="text-sm text-red-600">Your account has been {account.is_permanently_banned ? "permanently banned" : "temporarily suspended"}. Please contact support for assistance.</p>
          </div>
        ) : !account.setup_fee_paid ? (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center">
            <h2 className="text-xl font-bold text-amber-800 mb-2">Setup Fee Required</h2>
            <p className="text-sm text-amber-700 mb-4">Your account is pending setup fee payment ($15). Please complete the payment to access your dashboard.</p>
            <a href={`/partner?pay_setup=true&partner_id=${account.id}&email=${encodeURIComponent(account.email)}`}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-white hover:bg-primary-dark transition">
              Pay Now — $15
            </a>
          </div>
        ) : (
          <div className="space-y-8">
            {/* Stats Row */}
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="rounded-xl bg-white border border-gray-200 p-5">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-primary-light flex items-center justify-center">
                    <Wallet className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-xs text-text-muted">Balance</p>
                    <p className="text-xl font-bold text-text-dark">
                      ${account.balance.toFixed(2)}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl bg-white border border-gray-200 p-5">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-primary-light flex items-center justify-center">
                    <Smartphone className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-xs text-text-muted">Devices</p>
                    <p className="text-xl font-bold text-text-dark">
                      {account.device_count}{" "}
                      <span className="text-sm font-normal text-text-muted">
                        / {account.max_device_slots}
                      </span>
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl bg-white border border-gray-200 p-5">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-lg bg-primary-light flex items-center justify-center">
                    <Key className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="text-xs text-text-muted">Commission</p>
                    <p className="text-xl font-bold text-text-dark">
                      {account.commission_rate}%
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* API Credentials */}
            <div className="rounded-xl bg-white border border-gray-200 p-6">
              <h2 className="text-lg font-bold text-text-dark mb-4 flex items-center gap-2">
                <Key className="h-5 w-5 text-primary" />
                API Credentials
              </h2>
              <div className="space-y-4">
                {/* API Key */}
                <div>
                  <label className="block text-xs font-medium text-text-muted mb-1">
                    API Key
                  </label>
                  <div className="flex items-center gap-2">
                    <code className="flex-1 rounded-lg bg-gray-50 border border-gray-200 px-3 py-2.5 text-sm font-mono text-text-dark overflow-hidden">
                      {showApiKey
                        ? account.api_key
                        : maskValue(account.api_key)}
                    </code>
                    <button
                      onClick={() => setShowApiKey(!showApiKey)}
                      className="h-10 w-10 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
                    >
                      {showApiKey ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() =>
                        copyToClipboard(account.api_key, "api_key")
                      }
                      className="h-10 w-10 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
                    >
                      {copiedField === "api_key" ? (
                        <Check className="h-4 w-4 text-green-500" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>

                {/* API Secret */}
                <div>
                  <label className="block text-xs font-medium text-text-muted mb-1">
                    API Secret
                  </label>
                  <div className="flex items-center gap-2">
                    <code className="flex-1 rounded-lg bg-gray-50 border border-gray-200 px-3 py-2.5 text-sm font-mono text-text-dark overflow-hidden">
                      {showApiSecret
                        ? account.api_secret
                        : maskValue(account.api_secret)}
                    </code>
                    <button
                      onClick={() => setShowApiSecret(!showApiSecret)}
                      className="h-10 w-10 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
                    >
                      {showApiSecret ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() =>
                        copyToClipboard(account.api_secret, "api_secret")
                      }
                      className="h-10 w-10 flex items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors"
                    >
                      {copiedField === "api_secret" ? (
                        <Check className="h-4 w-4 text-green-500" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>
              </div>
              <p className="mt-4 text-xs text-text-muted">
                Use the API key in the{" "}
                <code className="bg-gray-100 px-1 rounded">X-API-Key</code>{" "}
                header for delivery API requests.
              </p>
            </div>

            {/* Device Credentials */}
            <div className="rounded-xl bg-white border border-gray-200 p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-bold text-text-dark flex items-center gap-2">
                  <Smartphone className="h-5 w-5 text-primary" />
                  Device Credentials
                  <span className="text-sm font-normal text-text-muted">
                    ({devices.length}/{account.max_device_slots})
                  </span>
                </h2>
                <div className="flex items-center gap-3">
                  <button
                    onClick={handlePurchaseSlots}
                    disabled={loading}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-text-dark hover:bg-gray-50 transition-colors disabled:opacity-50"
                  >
                    <ShoppingCart className="h-3.5 w-3.5" />
                    Buy 7 More ($5)
                  </button>
                  {devices.length < account.max_device_slots && (
                    <button
                      onClick={() => setShowCreateForm(!showCreateForm)}
                      className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-white hover:bg-primary-dark transition-colors"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Add Device
                    </button>
                  )}
                </div>
              </div>

              {/* Create Device Form */}
              {showCreateForm && (
                <div className="mb-6 rounded-xl bg-primary-light border border-primary/10 p-5">
                  <h3 className="text-sm font-semibold text-text-dark mb-4">
                    New Device Credential
                  </h3>
                  <form
                    onSubmit={handleCreateDevice}
                    className="grid gap-4 sm:grid-cols-3"
                  >
                    <div>
                      <label className="block text-xs font-medium text-text-muted mb-1">
                        Label
                      </label>
                      <input
                        type="text"
                        required
                        value={deviceLabel}
                        onChange={(e) => setDeviceLabel(e.target.value)}
                        className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none"
                        placeholder="Store Device 1"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-text-muted mb-1">
                        Login Email
                      </label>
                      <input
                        type="email"
                        required
                        value={deviceEmail}
                        onChange={(e) => setDeviceEmail(e.target.value)}
                        className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none"
                        placeholder="device1@company.co.zw"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-text-muted mb-1">
                        Password
                      </label>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          required
                          minLength={6}
                          value={devicePassword}
                          onChange={(e) => setDevicePassword(e.target.value)}
                          className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none"
                          placeholder="Min. 6 chars"
                        />
                        <button
                          type="submit"
                          disabled={loading}
                          className="shrink-0 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-dark disabled:opacity-50 flex items-center gap-1"
                        >
                          {loading ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <>
                              Create
                              <ArrowRight className="h-3.5 w-3.5" />
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  </form>
                </div>
              )}

              {/* Device List */}
              {devices.length === 0 ? (
                <div className="text-center py-10 text-text-muted">
                  <Smartphone className="h-10 w-10 mx-auto mb-3 text-gray-300" />
                  <p className="text-sm">No device credentials yet.</p>
                  <p className="text-xs mt-1">
                    Click &ldquo;Add Device&rdquo; to create login credentials
                    for your staff devices.
                  </p>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-gray-100">
                        <th className="text-left py-3 px-2 text-xs font-semibold text-text-muted uppercase tracking-wider">
                          Label
                        </th>
                        <th className="text-left py-3 px-2 text-xs font-semibold text-text-muted uppercase tracking-wider">
                          Login Email
                        </th>
                        <th className="text-left py-3 px-2 text-xs font-semibold text-text-muted uppercase tracking-wider">
                          Status
                        </th>
                        <th className="text-left py-3 px-2 text-xs font-semibold text-text-muted uppercase tracking-wider">
                          Created
                        </th>
                        <th className="text-right py-3 px-2 text-xs font-semibold text-text-muted uppercase tracking-wider">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {devices.map((device) => (
                        <tr
                          key={device.id}
                          className="border-b border-gray-50 hover:bg-gray-50/50"
                        >
                          <td className="py-3 px-2 font-medium text-text-dark">
                            {device.label}
                          </td>
                          <td className="py-3 px-2 text-text-muted font-mono text-xs">
                            {device.email}
                          </td>
                          <td className="py-3 px-2">
                            <span
                              className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                                device.is_active
                                  ? "bg-green-100 text-green-700"
                                  : "bg-gray-100 text-gray-600"
                              }`}
                            >
                              <span
                                className={`h-1.5 w-1.5 rounded-full ${
                                  device.is_active
                                    ? "bg-green-500"
                                    : "bg-gray-400"
                                }`}
                              />
                              {device.is_active ? "Active" : "Disabled"}
                            </span>
                          </td>
                          <td className="py-3 px-2 text-text-muted text-xs">
                            {new Date(device.created_at).toLocaleDateString()}
                          </td>
                          <td className="py-3 px-2 text-right">
                            <button
                              onClick={() => handleToggleDevice(device.id)}
                              className="inline-flex items-center gap-1 text-xs text-text-muted hover:text-text-dark transition-colors"
                              title={
                                device.is_active ? "Disable" : "Enable"
                              }
                            >
                              {device.is_active ? (
                                <ToggleRight className="h-5 w-5 text-green-500" />
                              ) : (
                                <ToggleLeft className="h-5 w-5 text-gray-400" />
                              )}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
