"use client";

import React, { useEffect, useState, useCallback } from "react";
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { Users, UserCheck, Briefcase, Plus, Copy, Check, X, ToggleLeft, ToggleRight, DollarSign, History, ArrowUpCircle, ArrowDownCircle, Wallet } from "lucide-react";
import {
  StatsCard,
  DataTable,
  ChartCard,
  StatusBadge,
  LoadingSpinner,
  EmptyState,
} from "@/components/dashboard";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";

// ---------- Types ----------

interface DeliveryByPartner {
  partner_name: string;
  count: number;
}

interface StatusBreakdown {
  status: string;
  count: number;
}

interface Partner {
  id: string;
  name: string;
  deliveries_count: number;
  is_active: boolean;
  created_at: string;
  contact_email: string;
  phone_number: string;
  balance: number;
  setup_fee_paid: boolean;
  commission_rate: number;
  description: string;
  address: string;
  city: string;
  contact_person_name: string;
  contact_person_role: string;
  max_device_slots: number;
}

interface Transaction {
  id: string;
  created_at: string;
  type: string;
  amount: number;
  balance_after: number;
  description: string;
  // Present on delivery_charge transactions
  fare?: number;
  system_commission?: number;
  driver_share?: number;
  driver_name?: string;
}

interface PartnerMetrics {
  total_partners: number;
  active_partners: number;
  deliveries_per_partner: DeliveryByPartner[];
  by_status: StatusBreakdown[];
  partner_list: Partner[];
}

// ---------- Constants ----------

const CHART_COLORS = [
  "#00a4e4",
  "#0e74bc",
  "#FF6B35",
  "#10b981",
  "#8b5cf6",
  "#f59e0b",
];

// ---------- Table columns (generated inside component for toggle access) ----------

// Use Record<string, unknown> for column render types to satisfy DataTable's generic constraint.
type PartnerRow = Record<string, unknown>;

// ---------- Custom Tooltip for Pie Chart ----------

interface PieTooltipProps {
  active?: boolean;
  payload?: Array<{ name: string; value: number }>;
}

function PieTooltipContent({ active, payload }: PieTooltipProps) {
  if (!active || !payload || payload.length === 0) return null;
  const entry = payload[0];
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-md">
      <p className="text-sm font-medium text-text-dark">{entry.name}</p>
      <p className="text-sm text-text-muted">
        {entry.value} {entry.value === 1 ? "delivery" : "deliveries"}
      </p>
    </div>
  );
}

// ---------- Create Partner Modal ----------

interface CreatePartnerModalProps {
  createForm: { name: string; contact_email: string; webhook_url: string; rate_limit: number };
  setCreateForm: React.Dispatch<React.SetStateAction<{ name: string; contact_email: string; webhook_url: string; rate_limit: number }>>;
  createLoading: boolean;
  createError: string | null;
  createdCredentials: { api_key: string; api_secret: string } | null;
  copiedField: string | null;
  onSubmit: (e: React.FormEvent) => void;
  onCopy: (text: string, field: string) => void;
  onClose: () => void;
}

function CreatePartnerModal({
  createForm,
  setCreateForm,
  createLoading,
  createError,
  createdCredentials,
  copiedField,
  onSubmit,
  onCopy,
  onClose,
}: CreatePartnerModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Dark overlay */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal card */}
      <div className="relative z-10 w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl mx-4">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-text-muted transition-colors hover:bg-gray-100 hover:text-text-dark"
        >
          <X className="h-5 w-5" />
        </button>

        {createdCredentials ? (
          /* Success view — show credentials */
          <div className="space-y-5">
            <div>
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
                <Check className="h-6 w-6 text-green-600" />
              </div>
              <h2 className="text-center text-lg font-bold text-text-dark">
                Partner Created Successfully
              </h2>
              <p className="mt-1 text-center text-sm text-text-muted">
                Save these credentials now. They will only be shown once.
              </p>
            </div>

            {/* API Key */}
            <div>
              <label className="mb-1 block text-xs font-medium text-text-muted uppercase tracking-wide">
                API Key
              </label>
              <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5">
                <code className="flex-1 break-all text-sm font-mono text-text-dark">
                  {createdCredentials.api_key}
                </code>
                <button
                  onClick={() => onCopy(createdCredentials.api_key, "api_key")}
                  className="shrink-0 rounded-md p-1.5 text-text-muted transition-colors hover:bg-gray-200 hover:text-text-dark"
                  title="Copy API Key"
                >
                  {copiedField === "api_key" ? (
                    <Check className="h-4 w-4 text-green-600" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            {/* API Secret */}
            <div>
              <label className="mb-1 block text-xs font-medium text-text-muted uppercase tracking-wide">
                API Secret
              </label>
              <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5">
                <code className="flex-1 break-all text-sm font-mono text-text-dark">
                  {createdCredentials.api_secret}
                </code>
                <button
                  onClick={() => onCopy(createdCredentials.api_secret, "api_secret")}
                  className="shrink-0 rounded-md p-1.5 text-text-muted transition-colors hover:bg-gray-200 hover:text-text-dark"
                  title="Copy API Secret"
                >
                  {copiedField === "api_secret" ? (
                    <Check className="h-4 w-4 text-green-600" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-700">
              These credentials cannot be retrieved again. Make sure to store them securely.
            </div>

            <button
              onClick={onClose}
              className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition hover:bg-primary-dark"
            >
              Done
            </button>
          </div>
        ) : (
          /* Create form */
          <div className="space-y-5">
            <div>
              <h2 className="text-lg font-bold text-text-dark">
                Create New Partner
              </h2>
              <p className="mt-1 text-sm text-text-muted">
                Add a new B2B partner to the platform.
              </p>
            </div>

            {createError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
                {createError}
              </div>
            )}

            <form onSubmit={onSubmit} className="space-y-4">
              {/* Company Name */}
              <div>
                <label
                  htmlFor="partner-name"
                  className="mb-1 block text-sm font-medium text-text-dark"
                >
                  Company Name <span className="text-red-500">*</span>
                </label>
                <input
                  id="partner-name"
                  type="text"
                  required
                  value={createForm.name}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, name: e.target.value }))
                  }
                  className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                  placeholder="Acme Corp"
                />
              </div>

              {/* Contact Email */}
              <div>
                <label
                  htmlFor="partner-email"
                  className="mb-1 block text-sm font-medium text-text-dark"
                >
                  Contact Email <span className="text-red-500">*</span>
                </label>
                <input
                  id="partner-email"
                  type="email"
                  required
                  value={createForm.contact_email}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, contact_email: e.target.value }))
                  }
                  className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                  placeholder="partner@company.com"
                />
              </div>

              {/* Webhook URL */}
              <div>
                <label
                  htmlFor="partner-webhook"
                  className="mb-1 block text-sm font-medium text-text-dark"
                >
                  Webhook URL <span className="text-xs text-text-muted font-normal">(optional)</span>
                </label>
                <input
                  id="partner-webhook"
                  type="url"
                  value={createForm.webhook_url}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, webhook_url: e.target.value }))
                  }
                  className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                  placeholder="https://api.company.com/webhooks/tumago"
                />
              </div>

              {/* Rate Limit */}
              <div>
                <label
                  htmlFor="partner-ratelimit"
                  className="mb-1 block text-sm font-medium text-text-dark"
                >
                  Rate Limit <span className="text-xs text-text-muted font-normal">(requests/min, default 100)</span>
                </label>
                <input
                  id="partner-ratelimit"
                  type="number"
                  min={1}
                  max={10000}
                  value={createForm.rate_limit}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, rate_limit: parseInt(e.target.value) || 100 }))
                  }
                  className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <button
                type="submit"
                disabled={createLoading || !createForm.name || !createForm.contact_email}
                className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {createLoading ? "Creating..." : "Create Partner"}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}

// ---------- Deposit Modal ----------

interface DepositModalProps {
  partner: Partner;
  depositAmount: string;
  depositDescription: string;
  setDepositAmount: (v: string) => void;
  setDepositDescription: (v: string) => void;
  depositLoading: boolean;
  depositError: string | null;
  depositSuccess: number | null;
  onSubmit: (e: React.FormEvent) => void;
  onClose: () => void;
}

function DepositModal({
  partner,
  depositAmount,
  depositDescription,
  setDepositAmount,
  setDepositDescription,
  depositLoading,
  depositError,
  depositSuccess,
  onSubmit,
  onClose,
}: DepositModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl mx-4">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-text-muted transition-colors hover:bg-gray-100 hover:text-text-dark"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="space-y-5">
          <div>
            <h2 className="text-lg font-bold text-text-dark">Deposit Funds</h2>
            <p className="mt-1 text-sm text-text-muted">{partner.name}</p>
          </div>

          {/* Current balance display */}
          <div className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-center">
            <p className="text-xs font-medium text-text-muted uppercase tracking-wide mb-1">
              Current Balance
            </p>
            <p
              className={`text-2xl font-bold ${
                partner.balance > 0 ? "text-emerald-600" : "text-red-600"
              }`}
            >
              ${partner.balance.toFixed(2)}
            </p>
          </div>

          {depositSuccess !== null ? (
            <div className="space-y-4">
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-center">
                <Check className="mx-auto h-6 w-6 text-emerald-600 mb-1" />
                <p className="text-sm font-medium text-emerald-700">
                  Deposit successful
                </p>
                <p className="text-lg font-bold text-emerald-700 mt-1">
                  New balance: ${depositSuccess.toFixed(2)}
                </p>
              </div>
              <button
                onClick={onClose}
                className="w-full rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-white transition hover:bg-primary-dark"
              >
                Done
              </button>
            </div>
          ) : (
            <>
              {depositError && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700">
                  {depositError}
                </div>
              )}

              <form onSubmit={onSubmit} className="space-y-4">
                <div>
                  <label
                    htmlFor="deposit-amount"
                    className="mb-1 block text-sm font-medium text-text-dark"
                  >
                    Amount ($) <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="deposit-amount"
                    type="number"
                    required
                    min={1}
                    step="0.01"
                    value={depositAmount}
                    onChange={(e) => setDepositAmount(e.target.value)}
                    className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                    placeholder="100.00"
                  />
                </div>

                <div>
                  <label
                    htmlFor="deposit-description"
                    className="mb-1 block text-sm font-medium text-text-dark"
                  >
                    Description{" "}
                    <span className="text-xs text-text-muted font-normal">
                      (optional)
                    </span>
                  </label>
                  <input
                    id="deposit-description"
                    type="text"
                    value={depositDescription}
                    onChange={(e) => setDepositDescription(e.target.value)}
                    className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm text-text-dark outline-none transition-colors placeholder:text-text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                    placeholder="Bank transfer #12345"
                  />
                </div>

                <button
                  type="submit"
                  disabled={depositLoading || !depositAmount || parseFloat(depositAmount) < 1}
                  className="w-full rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {depositLoading ? "Processing..." : "Deposit"}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ---------- Transaction Panel (Slide-out) ----------

interface TransactionPanelProps {
  partner: Partner;
  transactions: Transaction[];
  transactionsLoading: boolean;
  transactionsError: string | null;
  transactionPage: number;
  transactionHasMore: boolean;
  onPageChange: (page: number) => void;
  onClose: () => void;
}

function TransactionPanel({
  partner,
  transactions,
  transactionsLoading,
  transactionsError,
  transactionPage,
  transactionHasMore,
  onPageChange,
  onClose,
}: TransactionPanelProps) {
  const typeBadgeVariant = (type: string): "success" | "warning" | "danger" | "info" | "neutral" => {
    switch (type) {
      case "deposit":
        return "success";
      case "delivery_charge":
        return "danger";
      case "setup_fee":
        return "warning";
      case "refund":
        return "info";
      default:
        return "neutral";
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-xl bg-white shadow-2xl overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
          <div>
            <h2 className="text-lg font-bold text-text-dark">
              Transaction History
            </h2>
            <p className="text-sm text-text-muted">{partner.name}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-text-muted transition-colors hover:bg-gray-100 hover:text-text-dark"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          {transactionsLoading ? (
            <div className="flex items-center justify-center py-20">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            </div>
          ) : transactionsError ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 text-center">
              {transactionsError}
            </div>
          ) : transactions.length === 0 ? (
            <div className="py-20 text-center text-sm text-text-muted">
              No transactions found
            </div>
          ) : (
            <>
              <div className="space-y-3">
                {transactions.map((tx) => (
                  <div
                    key={tx.id}
                    className="rounded-lg border border-gray-100 p-4 transition-colors hover:bg-gray-50"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-2.5">
                        {tx.amount >= 0 ? (
                          <ArrowUpCircle className="h-5 w-5 shrink-0 text-emerald-500" />
                        ) : (
                          <ArrowDownCircle className="h-5 w-5 shrink-0 text-red-500" />
                        )}
                        <div>
                          <div className="flex items-center gap-2">
                            <StatusBadge
                              status={tx.type.replace(/_/g, " ")}
                              variant={typeBadgeVariant(tx.type)}
                            />
                          </div>
                          <p className="mt-1 text-xs text-text-muted">
                            {new Date(tx.created_at).toLocaleDateString("en-US", {
                              year: "numeric",
                              month: "short",
                              day: "numeric",
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p
                          className={`text-sm font-bold ${
                            tx.amount >= 0 ? "text-emerald-600" : "text-red-600"
                          }`}
                        >
                          {tx.amount >= 0 ? "+" : ""}${Math.abs(tx.amount).toFixed(2)}
                        </p>
                        <p className="text-xs text-text-muted">
                          Bal: ${tx.balance_after.toFixed(2)}
                        </p>
                      </div>
                    </div>

                    {tx.description && (
                      <p className="mt-2 text-xs text-text-muted">
                        {tx.description}
                      </p>
                    )}

                    {/* Delivery charge breakdown */}
                    {tx.type === "delivery_charge" && tx.fare != null && (
                      <div className="mt-2 rounded-md bg-gray-50 px-3 py-2 text-xs text-text-muted">
                        <div className="flex flex-wrap gap-x-4 gap-y-1">
                          <span>
                            Fare: <strong>${tx.fare.toFixed(2)}</strong>
                          </span>
                          {tx.system_commission != null && (
                            <span>
                              Commission:{" "}
                              <strong>${tx.system_commission.toFixed(2)}</strong>
                            </span>
                          )}
                          {tx.driver_share != null && (
                            <span>
                              Driver:{" "}
                              <strong>${tx.driver_share.toFixed(2)}</strong>
                            </span>
                          )}
                          {tx.driver_name && (
                            <span>
                              Driver: <strong>{tx.driver_name}</strong>
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {/* Pagination */}
              <div className="mt-4 flex items-center justify-between">
                <button
                  onClick={() => onPageChange(transactionPage - 1)}
                  disabled={transactionPage <= 1}
                  className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-text-muted transition hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-xs text-text-muted">
                  Page {transactionPage}
                </span>
                <button
                  onClick={() => onPageChange(transactionPage + 1)}
                  disabled={!transactionHasMore}
                  className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-text-muted transition hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ---------- Partner Detail Panel ----------

function PartnerDetailPanel({ partner, onClose }: { partner: Partner; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="relative z-10 w-full max-w-lg bg-white shadow-2xl overflow-y-auto">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
          <div>
            <h2 className="text-lg font-bold text-text-dark">{partner.name}</h2>
            <p className="text-sm text-text-muted">Partner Details</p>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 text-text-muted transition-colors hover:bg-gray-100 hover:text-text-dark">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6 space-y-6">
          {/* Status */}
          <div className="flex items-center gap-3">
            <StatusBadge status={partner.is_active ? "Active" : "Inactive"} variant={partner.is_active ? "success" : "danger"} />
            <StatusBadge status={partner.setup_fee_paid ? "Setup Paid" : "Setup Pending"} variant={partner.setup_fee_paid ? "success" : "warning"} />
          </div>

          {/* Contact Person */}
          {partner.contact_person_name && (
            <div className="rounded-lg border border-gray-200 p-4">
              <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-3">
                Registered By
              </h3>
              <p className="text-sm font-semibold text-text-dark">{partner.contact_person_name}</p>
              {partner.contact_person_role && (
                <p className="text-xs text-text-muted mt-0.5">{partner.contact_person_role}</p>
              )}
            </div>
          )}

          {/* Business Info */}
          <div className="rounded-lg border border-gray-200 p-4 space-y-3">
            <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider">
              Business Information
            </h3>
            {partner.description && (
              <div>
                <p className="text-xs text-text-muted">Description</p>
                <p className="text-sm text-text-dark mt-0.5">{partner.description}</p>
              </div>
            )}
            {(partner.address || partner.city) && (
              <div>
                <p className="text-xs text-text-muted">Address</p>
                <p className="text-sm text-text-dark mt-0.5">
                  {[partner.address, partner.city].filter(Boolean).join(", ")}
                </p>
              </div>
            )}
          </div>

          {/* Contact Info */}
          <div className="rounded-lg border border-gray-200 p-4 space-y-3">
            <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider">
              Contact Information
            </h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-xs text-text-muted">Email</p>
                <p className="text-sm text-text-dark mt-0.5">{partner.contact_email}</p>
              </div>
              {partner.phone_number && (
                <div>
                  <p className="text-xs text-text-muted">Phone</p>
                  <p className="text-sm text-text-dark mt-0.5">{partner.phone_number}</p>
                </div>
              )}
            </div>
          </div>

          {/* Account Stats */}
          <div className="rounded-lg border border-gray-200 p-4 space-y-3">
            <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider">
              Account
            </h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-xs text-text-muted">Balance</p>
                <p className={`text-sm font-semibold mt-0.5 ${partner.balance > 0 ? "text-emerald-600" : "text-red-600"}`}>
                  ${partner.balance.toFixed(2)}
                </p>
              </div>
              <div>
                <p className="text-xs text-text-muted">Commission Rate</p>
                <p className="text-sm text-text-dark mt-0.5">{partner.commission_rate}%</p>
              </div>
              <div>
                <p className="text-xs text-text-muted">Deliveries</p>
                <p className="text-sm text-text-dark mt-0.5">{partner.deliveries_count}</p>
              </div>
              <div>
                <p className="text-xs text-text-muted">Device Slots</p>
                <p className="text-sm text-text-dark mt-0.5">{partner.max_device_slots}</p>
              </div>
            </div>
          </div>

          {/* Dates */}
          <div className="text-xs text-text-muted">
            Member since {new Date(partner.created_at).toLocaleDateString("en-US", { year: "numeric", month: "long", day: "numeric" })}
          </div>
        </div>
      </div>
    </div>
  );
}


// ---------- Page Component ----------

export default function PartnersPage() {
  const { token } = useAuth();
  const [data, setData] = useState<PartnerMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  // Create partner modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    name: "",
    contact_email: "",
    webhook_url: "",
    rate_limit: 100,
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Success view state (shows api_key and api_secret)
  const [createdCredentials, setCreatedCredentials] = useState<{
    api_key: string;
    api_secret: string;
  } | null>(null);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  // Toggle loading state per partner
  const [togglingPartner, setTogglingPartner] = useState<string | null>(null);

  // Deposit modal state
  const [depositPartner, setDepositPartner] = useState<Partner | null>(null);
  const [depositAmount, setDepositAmount] = useState("");
  const [depositDescription, setDepositDescription] = useState("");
  const [depositLoading, setDepositLoading] = useState(false);
  const [depositError, setDepositError] = useState<string | null>(null);
  const [depositSuccess, setDepositSuccess] = useState<number | null>(null);

  // Detail panel state
  const [detailPartner, setDetailPartner] = useState<Partner | null>(null);

  // Transaction panel state
  const [txPartner, setTxPartner] = useState<Partner | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [txLoading, setTxLoading] = useState(false);
  const [txError, setTxError] = useState<string | null>(null);
  const [txPage, setTxPage] = useState(1);
  const [txHasMore, setTxHasMore] = useState(false);

  const fetchData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await adminAPI.partnerMetrics(token);
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load partner data");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // ---------- Create partner handler ----------

  const handleCreatePartner = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;
    setCreateLoading(true);
    setCreateError(null);

    try {
      const payload: { name: string; contact_email: string; webhook_url?: string; rate_limit?: number } = {
        name: createForm.name,
        contact_email: createForm.contact_email,
      };
      if (createForm.webhook_url) payload.webhook_url = createForm.webhook_url;
      if (createForm.rate_limit !== 100) payload.rate_limit = createForm.rate_limit;

      const result = await adminAPI.createPartner(token, payload);
      setCreatedCredentials({
        api_key: result.api_key,
        api_secret: result.api_secret,
      });
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : "Failed to create partner");
    } finally {
      setCreateLoading(false);
    }
  };

  const handleCloseModal = () => {
    setShowCreateModal(false);
    setCreateForm({ name: "", contact_email: "", webhook_url: "", rate_limit: 100 });
    setCreateError(null);
    if (createdCredentials) {
      setCreatedCredentials(null);
      fetchData(); // Refresh partner list after successful creation
    }
  };

  const handleCopy = async (text: string, field: string) => {
    await navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  // ---------- Toggle partner handler ----------

  const handleTogglePartner = async (partnerId: string) => {
    if (!token) return;
    setTogglingPartner(partnerId);
    try {
      await adminAPI.togglePartner(token, partnerId);
      fetchData(); // Refresh to get updated state
    } catch {
      // Silently handle — the toggle will revert on refresh
    } finally {
      setTogglingPartner(null);
    }
  };

  // ---------- Deposit handler ----------

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || !depositPartner) return;
    setDepositLoading(true);
    setDepositError(null);

    try {
      const payload: { amount: number; description?: string } = {
        amount: parseFloat(depositAmount),
      };
      if (depositDescription.trim()) payload.description = depositDescription.trim();

      const result = await adminAPI.partnerDeposit(token, depositPartner.id, payload);
      setDepositSuccess(result.new_balance ?? parseFloat(depositAmount) + depositPartner.balance);
      fetchData(); // Refresh table with new balance
    } catch (err) {
      setDepositError(err instanceof Error ? err.message : "Failed to process deposit");
    } finally {
      setDepositLoading(false);
    }
  };

  const handleCloseDeposit = () => {
    setDepositPartner(null);
    setDepositAmount("");
    setDepositDescription("");
    setDepositError(null);
    setDepositSuccess(null);
  };

  // ---------- Transaction fetch ----------

  const fetchTransactions = useCallback(
    async (partner: Partner, page: number) => {
      if (!token) return;
      setTxLoading(true);
      setTxError(null);

      try {
        const result = await adminAPI.partnerTransactions(
          token,
          partner.id,
          `page=${page}`
        );
        setTransactions(result.results ?? result.transactions ?? []);
        setTxHasMore(!!result.next);
      } catch (err) {
        setTxError(
          err instanceof Error ? err.message : "Failed to load transactions"
        );
      } finally {
        setTxLoading(false);
      }
    },
    [token]
  );

  const handleOpenTransactions = (partner: Partner) => {
    setTxPartner(partner);
    setTxPage(1);
    fetchTransactions(partner, 1);
  };

  const handleTxPageChange = (page: number) => {
    if (!txPartner || page < 1) return;
    setTxPage(page);
    fetchTransactions(txPartner, page);
  };

  const handleCloseTransactions = () => {
    setTxPartner(null);
    setTransactions([]);
    setTxError(null);
    setTxPage(1);
  };

  // ---------- Table columns (includes toggle) ----------

  const partnerColumns = [
    {
      key: "name",
      header: "Name",
      sortable: true,
    },
    {
      key: "contact_email",
      header: "Email",
      sortable: true,
    },
    {
      key: "balance",
      header: "Balance",
      sortable: true,
      render: (item: PartnerRow) => {
        const balance = (item.balance as number) ?? 0;
        return (
          <span
            className={`font-semibold ${
              balance > 0 ? "text-emerald-600" : "text-red-600"
            }`}
          >
            ${balance.toFixed(2)}
          </span>
        );
      },
    },
    {
      key: "setup_fee_paid",
      header: "Setup Fee",
      render: (item: PartnerRow) => (
        <StatusBadge
          status={item.setup_fee_paid ? "Setup Paid" : "Setup Pending"}
          variant={item.setup_fee_paid ? "success" : "warning"}
        />
      ),
    },
    {
      key: "deliveries_count",
      header: "Deliveries",
      sortable: true,
    },
    {
      key: "is_active",
      header: "Status",
      render: (item: PartnerRow) => (
        <div className="flex items-center gap-2">
          <StatusBadge
            status={item.is_active ? "Active" : "Inactive"}
            variant={item.is_active ? "success" : "danger"}
          />
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleTogglePartner(item.id as string);
            }}
            disabled={togglingPartner === (item.id as string)}
            className="text-text-muted hover:text-primary transition-colors disabled:opacity-50"
            title={item.is_active ? "Deactivate partner" : "Activate partner"}
          >
            {item.is_active ? (
              <ToggleRight className="h-5 w-5 text-primary" />
            ) : (
              <ToggleLeft className="h-5 w-5" />
            )}
          </button>
        </div>
      ),
    },
    {
      key: "created_at",
      header: "Member Since",
      sortable: true,
      render: (item: PartnerRow) => {
        if (!item.created_at) return "\u2014";
        return new Date(item.created_at as string).toLocaleDateString("en-US", {
          year: "numeric",
          month: "short",
          day: "numeric",
        });
      },
    },
    {
      key: "actions",
      header: "Actions",
      render: (item: PartnerRow) => {
        const partner = item as unknown as Partner;
        return (
          <div className="flex items-center gap-1">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setDetailPartner(partner);
              }}
              className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-primary bg-primary-light transition-colors hover:bg-blue-100"
              title="View Details"
            >
              View
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setDepositPartner(partner);
              }}
              className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-emerald-700 bg-emerald-50 transition-colors hover:bg-emerald-100"
              title="Deposit Funds"
            >
              <DollarSign className="h-3.5 w-3.5" />
              Deposit
            </button>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleOpenTransactions(partner);
              }}
              className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-blue-700 bg-blue-50 transition-colors hover:bg-blue-100"
              title="View Transactions"
            >
              <History className="h-3.5 w-3.5" />
              Transactions
            </button>
          </div>
        );
      },
    },
  ];

  // ---------- Loading state ----------

  if (loading) {
    return <LoadingSpinner text="Loading partner analytics..." size="lg" />;
  }

  // ---------- Error state ----------

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-text-dark">
          B2B Partner Analytics
        </h1>
        <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-center">
          <p className="text-sm font-medium text-red-700">{error}</p>
          <button
            onClick={fetchData}
            className="mt-3 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  // ---------- Empty state ----------

  if (!data || data.total_partners === 0) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-text-dark">
            B2B Partner Analytics
          </h1>
          <button
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition hover:bg-primary-dark"
          >
            <Plus className="h-4 w-4" />
            Create Partner
          </button>
        </div>
        <EmptyState
          icon={<Briefcase className="h-7 w-7" />}
          title="No B2B partners yet"
          description="Partner analytics will appear here once businesses start using TumaGo for deliveries."
        />

        {/* Create Partner Modal (also available in empty state) */}
        {showCreateModal && (
          <CreatePartnerModal
            createForm={createForm}
            setCreateForm={setCreateForm}
            createLoading={createLoading}
            createError={createError}
            createdCredentials={createdCredentials}
            copiedField={copiedField}
            onSubmit={handleCreatePartner}
            onCopy={handleCopy}
            onClose={handleCloseModal}
          />
        )}
      </div>
    );
  }

  // ---------- Filtered partner list for the table ----------

  const filteredPartners = searchQuery
    ? data.partner_list.filter(
        (p) =>
          p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          p.contact_email.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : data.partner_list;

  // ---------- Render ----------

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-dark">
            B2B Partner Analytics
          </h1>
          <p className="mt-1 text-sm text-text-muted">
            Track partner performance and delivery distribution
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 self-start rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition hover:bg-primary-dark"
        >
          <Plus className="h-4 w-4" />
          Create Partner
        </button>
      </div>

      {/* Create Partner Modal */}
      {showCreateModal && (
        <CreatePartnerModal
          createForm={createForm}
          setCreateForm={setCreateForm}
          createLoading={createLoading}
          createError={createError}
          createdCredentials={createdCredentials}
          copiedField={copiedField}
          onSubmit={handleCreatePartner}
          onCopy={handleCopy}
          onClose={handleCloseModal}
        />
      )}

      {/* Stats row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatsCard
          title="Total Partners"
          value={data.total_partners}
          icon={<Users className="h-5 w-5" />}
          subtitle="All registered B2B partners"
        />
        <StatsCard
          title="Active Partners"
          value={data.active_partners}
          icon={<UserCheck className="h-5 w-5" />}
          subtitle="Partners with active status"
          change={
            data.total_partners > 0
              ? `${Math.round((data.active_partners / data.total_partners) * 100)}% of total`
              : undefined
          }
          changeType="positive"
        />
        <StatsCard
          title="Total Partner Balance"
          value={`$${data.partner_list.reduce((sum, p) => sum + (p.balance ?? 0), 0).toFixed(2)}`}
          icon={<Wallet className="h-5 w-5" />}
          subtitle="Combined balance across all partners"
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Bar Chart — Deliveries by Partner */}
        <ChartCard
          title="Deliveries by Partner"
          subtitle="Total delivery count per partner"
        >
          {data.deliveries_per_partner.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <BarChart
                data={data.deliveries_per_partner}
                margin={{ top: 10, right: 10, left: 0, bottom: 40 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="partner_name"
                  tick={{ fontSize: 12, fill: "#6b7280" }}
                  angle={-35}
                  textAnchor="end"
                  interval={0}
                  height={60}
                />
                <YAxis
                  tick={{ fontSize: 12, fill: "#6b7280" }}
                  allowDecimals={false}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: "8px",
                    border: "1px solid #e5e7eb",
                    boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1)",
                  }}
                />
                <Bar
                  dataKey="count"
                  name="Deliveries"
                  fill="#00a4e4"
                  radius={[4, 4, 0, 0]}
                  maxBarSize={60}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-[320px] items-center justify-center text-sm text-text-muted">
              No delivery data available
            </div>
          )}
        </ChartCard>

        {/* Pie Chart — Delivery Status Distribution */}
        <ChartCard
          title="Delivery Status Distribution"
          subtitle="Breakdown of delivery statuses across partners"
        >
          {data.by_status.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <PieChart>
                <Pie
                  data={data.by_status}
                  dataKey="count"
                  nameKey="status"
                  cx="50%"
                  cy="50%"
                  outerRadius={110}
                  innerRadius={55}
                  paddingAngle={3}
                  label={({ status, percent }) =>
                    `${status} (${(percent * 100).toFixed(0)}%)`
                  }
                  labelLine={{ stroke: "#9ca3af", strokeWidth: 1 }}
                >
                  {data.by_status.map((_, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={CHART_COLORS[index % CHART_COLORS.length]}
                    />
                  ))}
                </Pie>
                <Tooltip content={<PieTooltipContent />} />
                <Legend
                  verticalAlign="bottom"
                  iconType="circle"
                  iconSize={8}
                  formatter={(value: string) => (
                    <span className="text-xs text-text-muted capitalize">
                      {value}
                    </span>
                  )}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-[320px] items-center justify-center text-sm text-text-muted">
              No status data available
            </div>
          )}
        </ChartCard>
      </div>

      {/* Partner Directory Table */}
      <div>
        <h2 className="mb-3 text-lg font-semibold text-text-dark">
          Partner Directory
        </h2>
        <DataTable
          columns={partnerColumns}
          data={filteredPartners as unknown as PartnerRow[]}
          onSearch={setSearchQuery}
          searchPlaceholder="Search partners by name or email..."
        />
      </div>

      {/* Deposit Modal */}
      {depositPartner && (
        <DepositModal
          partner={depositPartner}
          depositAmount={depositAmount}
          depositDescription={depositDescription}
          setDepositAmount={setDepositAmount}
          setDepositDescription={setDepositDescription}
          depositLoading={depositLoading}
          depositError={depositError}
          depositSuccess={depositSuccess}
          onSubmit={handleDeposit}
          onClose={handleCloseDeposit}
        />
      )}

      {/* Partner Detail Panel */}
      {detailPartner && (
        <PartnerDetailPanel
          partner={detailPartner}
          onClose={() => setDetailPartner(null)}
        />
      )}

      {/* Transaction Panel */}
      {txPartner && (
        <TransactionPanel
          partner={txPartner}
          transactions={transactions}
          transactionsLoading={txLoading}
          transactionsError={txError}
          transactionPage={txPage}
          transactionHasMore={txHasMore}
          onPageChange={handleTxPageChange}
          onClose={handleCloseTransactions}
        />
      )}
    </div>
  );
}
