"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";

interface PlatformSettings {
  scooter_price_per_km: number;
  scooter_base_fee: number;
  van_price_per_km: number;
  van_base_fee: number;
  truck_price_per_km: number;
  truck_base_fee: number;
  cash_commission_pct: number;
  online_commission_pct: number;
  updated_at: string | null;
}

const PRICING_FIELDS = [
  { key: "scooter_price_per_km", label: "Scooter $/km", group: "scooter" },
  { key: "scooter_base_fee", label: "Scooter base fee", group: "scooter" },
  { key: "van_price_per_km", label: "Van $/km", group: "van" },
  { key: "van_base_fee", label: "Van base fee", group: "van" },
  { key: "truck_price_per_km", label: "Truck $/km", group: "truck" },
  { key: "truck_base_fee", label: "Truck base fee", group: "truck" },
] as const;

const COMMISSION_FIELDS = [
  { key: "cash_commission_pct", label: "Cash commission %" },
  { key: "online_commission_pct", label: "Card/EcoCash/OneMoney commission %" },
] as const;

export default function SettingsPage() {
  const { token } = useAuth();
  const [settings, setSettings] = useState<PlatformSettings | null>(null);
  const [form, setForm] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (token) loadSettings();
  }, [token]);

  async function loadSettings() {
    if (!token) return;
    setLoading(true);
    try {
      const data = await adminAPI.getSettings(token);
      setSettings(data);
      const formData: Record<string, string> = {};
      for (const f of [...PRICING_FIELDS, ...COMMISSION_FIELDS]) {
        formData[f.key] = String(data[f.key] ?? "");
      }
      setForm(formData);
    } catch (err) {
      console.error("Failed to load settings:", err);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    if (!token) return;
    setSaving(true);
    setMessage("");
    try {
      const payload: Record<string, number> = {};
      for (const [key, value] of Object.entries(form)) {
        const num = parseFloat(value);
        if (isNaN(num) || num < 0) {
          setMessage(`Invalid value for ${key}`);
          setSaving(false);
          return;
        }
        payload[key] = num;
      }
      const result = await adminAPI.updateSettings(token, payload);
      setMessage(result.message || "Settings saved");
      loadSettings();
    } catch (err: unknown) {
      setMessage(err instanceof Error ? err.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }

  function handleChange(key: string, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Platform Settings</h1>
        {settings?.updated_at && (
          <p className="text-sm text-gray-500 mt-1">
            Last updated: {new Date(settings.updated_at).toLocaleString()}
          </p>
        )}
      </div>

      {/* Vehicle Pricing */}
      <div className="bg-white rounded-xl shadow-sm p-6 space-y-6">
        <h2 className="text-lg font-semibold text-gray-800">
          Vehicle Pricing
        </h2>
        <p className="text-sm text-gray-500">
          Fare = (price per km x distance) + base fee
        </p>

        {["scooter", "van", "truck"].map((vehicle) => (
          <div key={vehicle}>
            <h3 className="text-sm font-medium text-gray-700 capitalize mb-2">
              {vehicle}
            </h3>
            <div className="grid grid-cols-2 gap-4">
              {PRICING_FIELDS.filter((f) => f.group === vehicle).map((f) => (
                <div key={f.key}>
                  <label className="block text-xs text-gray-500 mb-1">
                    {f.label}
                  </label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                      $
                    </span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      value={form[f.key] ?? ""}
                      onChange={(e) => handleChange(f.key, e.target.value)}
                      className="w-full pl-7 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Commission Rates */}
      <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-800">
          Commission Rates
        </h2>
        <p className="text-sm text-gray-500">
          Percentage deducted from the driver's wallet per delivery
        </p>

        <div className="grid grid-cols-2 gap-4">
          {COMMISSION_FIELDS.map((f) => (
            <div key={f.key}>
              <label className="block text-xs text-gray-500 mb-1">
                {f.label}
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  max="100"
                  value={form[f.key] ?? ""}
                  onChange={(e) => handleChange(f.key, e.target.value)}
                  className="w-full pr-8 pl-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">
                  %
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Save */}
      <div className="flex items-center gap-4">
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-6 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
        >
          {saving ? "Saving..." : "Save Changes"}
        </button>
        {message && (
          <span
            className={`text-sm ${
              message.includes("fail") || message.includes("Invalid")
                ? "text-red-600"
                : "text-green-600"
            }`}
          >
            {message}
          </span>
        )}
      </div>
    </div>
  );
}
