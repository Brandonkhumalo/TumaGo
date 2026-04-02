"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";
import { FileText, Save, Loader2, CheckCircle, AlertCircle } from "lucide-react";

interface TermsData {
  id: number;
  content: string;
  version: number;
  updated_at: string;
}

export default function TermsPage() {
  const { token } = useAuth();
  const [clientTerms, setClientTerms] = useState<TermsData | null>(null);
  const [driverTerms, setDriverTerms] = useState<TermsData | null>(null);
  const [clientContent, setClientContent] = useState("");
  const [driverContent, setDriverContent] = useState("");
  const [activeTab, setActiveTab] = useState<"client" | "driver">("client");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ type: "success" | "error"; message: string } | null>(null);

  useEffect(() => {
    if (token) fetchTerms();
  }, [token]);

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  async function fetchTerms() {
    try {
      setLoading(true);
      const data = await adminAPI.getTerms(token!);
      setClientTerms(data.client);
      setDriverTerms(data.driver);
      setClientContent(data.client?.content || "");
      setDriverContent(data.driver?.content || "");
    } catch {
      setToast({ type: "error", message: "Failed to load terms" });
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(appType: "client" | "driver") {
    const content = appType === "client" ? clientContent : driverContent;
    if (!content.trim()) {
      setToast({ type: "error", message: "Content cannot be empty" });
      return;
    }

    try {
      setSaving(true);
      const result = await adminAPI.updateTerms(token!, { app_type: appType, content: content.trim() });
      if (appType === "client") {
        setClientTerms(result);
      } else {
        setDriverTerms(result);
      }
      setToast({
        type: "success",
        message: `${appType === "client" ? "Client" : "Driver"} T&C updated to v${result.version}. Apps will show the new version immediately.`,
      });
    } catch {
      setToast({ type: "error", message: "Failed to save terms" });
    } finally {
      setSaving(false);
    }
  }

  const current = activeTab === "client" ? clientTerms : driverTerms;
  const currentContent = activeTab === "client" ? clientContent : driverContent;
  const setCurrentContent = activeTab === "client" ? setClientContent : setDriverContent;
  const hasChanges =
    activeTab === "client"
      ? clientContent !== (clientTerms?.content || "")
      : driverContent !== (driverTerms?.content || "");

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Terms & Conditions</h1>
        <p className="text-sm text-slate-500 mt-1">
          Manage T&C for each app. When you update, users must re-accept before continuing.
        </p>
      </div>

      {/* Toast */}
      {toast && (
        <div
          className={`flex items-center gap-3 px-4 py-3 rounded-lg text-sm ${
            toast.type === "success"
              ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {toast.type === "success" ? (
            <CheckCircle className="w-4 h-4 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 shrink-0" />
          )}
          {toast.message}
        </div>
      )}

      {/* Tab switcher */}
      <div className="flex gap-1 bg-slate-200 rounded-lg p-1 w-fit">
        {(["client", "driver"] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-5 py-2 rounded-md text-sm font-medium transition cursor-pointer ${
              activeTab === tab
                ? "bg-white text-slate-900 shadow-sm"
                : "text-slate-500 hover:text-slate-700"
            }`}
          >
            {tab === "client" ? "Client App" : "Driver App"}
          </button>
        ))}
      </div>

      {/* Editor card */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        {/* Card header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
              <FileText className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h2 className="font-semibold text-slate-900">
                {activeTab === "client" ? "Client App" : "Driver App"} Terms
              </h2>
              <p className="text-xs text-slate-400">
                {current
                  ? `Version ${current.version} \u00B7 Last updated ${new Date(current.updated_at).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}`
                  : "No terms published yet"}
              </p>
            </div>
          </div>

          <button
            onClick={() => handleSave(activeTab)}
            disabled={saving || !hasChanges}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition cursor-pointer ${
              hasChanges
                ? "bg-blue-600 text-white hover:bg-blue-700"
                : "bg-slate-100 text-slate-400 cursor-not-allowed"
            }`}
          >
            {saving ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Save className="w-4 h-4" />
            )}
            {saving ? "Saving..." : current ? "Update & Publish" : "Publish"}
          </button>
        </div>

        {/* Textarea */}
        <div className="p-6">
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Terms & Conditions Content
          </label>
          <p className="text-xs text-slate-400 mb-3">
            Write the full terms and conditions text. Use blank lines to separate paragraphs.
            This will be displayed directly in the app.
          </p>
          <textarea
            value={currentContent}
            onChange={(e) => setCurrentContent(e.target.value)}
            rows={20}
            placeholder={`Type the ${activeTab === "client" ? "client" : "driver"} app terms and conditions here...`}
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-y font-mono leading-relaxed"
          />
        </div>

        {/* Footer info */}
        {hasChanges && (
          <div className="px-6 pb-4">
            <p className="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              You have unsaved changes. Publishing will create a new version and all users will be
              required to re-accept the updated terms on their next app open.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
