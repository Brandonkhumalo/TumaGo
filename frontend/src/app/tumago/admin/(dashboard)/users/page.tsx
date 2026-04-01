"use client";

import React, { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";
import {
  StatsCard,
  DataTable,
  ChartCard,
  StatusBadge,
  LoadingSpinner,
} from "@/components/dashboard";
import {
  Users,
  UserCheck,
  Repeat,
  UserPlus,
  Star,
  Eye,
  X,
  Ban,
  ShieldCheck,
  MapPin,
  Car,
  DollarSign,
  Calendar,
  Mail,
  Phone,
  User as UserIcon,
  AlertTriangle,
  Loader2,
} from "lucide-react";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

// ── Types ────────────────────────────────────────────────────────────────

/** Shorthand alias so column render functions satisfy DataTable's generic */
type Row = Record<string, unknown>;

interface SignupPoint {
  date: string;
  count: number;
}

interface TopSender {
  name: string;
  email: string;
  delivery_count: number;
}

interface UserMetrics {
  total_users: number;
  active_users: number;
  signups_over_time: SignupPoint[];
  repeat_users: number;
  top_senders: TopSender[];
}

interface UsersListResponse {
  results: Row[];
  total: number;
  page: number;
  total_pages: number;
}

interface UserDetail {
  id: string;
  name: string;
  surname: string;
  email: string;
  phone_number: string;
  role: string;
  rating: number;
  rating_count: number;
  date_joined: string;
  is_active: boolean;
  driver_online: boolean;
  driver_available: boolean;
  city: string;
  province: string;
  is_banned: boolean;
  banned_until: string | null;
  ban_reason: string;
  delivery_count: number;
  recent_deliveries: Array<{
    delivery_id: string;
    fare: number;
    payment_method: string;
    successful: boolean;
    date: string;
  }>;
  recent_payments: Array<{
    id: string;
    amount: number;
    payment_method: string;
    status: string;
    created_at: string;
  }>;
  driver_finances?: {
    earnings: number;
    charges: number;
    profit: number;
    total_trips: number;
  };
  driver_vehicle?: {
    delivery_vehicle: string;
    car_name: string;
    number_plate: string;
    color: string;
  };
  driver_balance?: {
    owed_amount: number;
    total_paid: number;
  };
}

type BanDuration = "permanent" | "1d" | "7d" | "30d" | "90d" | "custom";

// ── Helpers ──────────────────────────────────────────────────────────────

const CHART_COLORS = [
  "#00a4e4",
  "#0e74bc",
  "#FF6B35",
  "#10b981",
  "#8b5cf6",
  "#f59e0b",
];

function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
}

function formatDateTime(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return dateStr;
  }
}

function formatChartDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    });
  } catch {
    return dateStr;
  }
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

// ── Star Rating Component ────────────────────────────────────────────────

function StarRating({ rating, count }: { rating: number; count?: number }) {
  const fullStars = Math.floor(rating);
  const hasHalf = rating - fullStars >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalf ? 1 : 0);

  return (
    <span className="inline-flex items-center gap-1">
      {Array.from({ length: fullStars }).map((_, i) => (
        <Star
          key={`full-${i}`}
          className="h-4 w-4 fill-amber-400 text-amber-400"
        />
      ))}
      {hasHalf && (
        <span className="relative">
          <Star className="h-4 w-4 text-gray-300" />
          <span className="absolute inset-0 overflow-hidden w-1/2">
            <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
          </span>
        </span>
      )}
      {Array.from({ length: emptyStars }).map((_, i) => (
        <Star key={`empty-${i}`} className="h-4 w-4 text-gray-300" />
      ))}
      <span className="ml-1 text-sm font-medium text-text-dark">
        {rating.toFixed(1)}
      </span>
      {count != null && (
        <span className="text-xs text-text-muted">({count})</span>
      )}
    </span>
  );
}

// ── Success Toast ────────────────────────────────────────────────────────

function SuccessToast({
  message,
  onClose,
}: {
  message: string;
  onClose: () => void;
}) {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className="fixed top-6 right-6 z-[60] animate-in slide-in-from-top-2 fade-in duration-300">
      <div className="flex items-center gap-3 rounded-lg bg-emerald-600 px-4 py-3 text-white shadow-lg">
        <ShieldCheck className="h-5 w-5 flex-shrink-0" />
        <span className="text-sm font-medium">{message}</span>
        <button
          onClick={onClose}
          className="ml-2 rounded p-0.5 hover:bg-emerald-500 transition-colors"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

// ── Ban Modal ────────────────────────────────────────────────────────────

function BanModal({
  user,
  onClose,
  onBan,
}: {
  user: UserDetail;
  onClose: () => void;
  onBan: (reason: string, duration: string, customUntil?: string) => Promise<void>;
}) {
  const [reason, setReason] = useState("");
  const [duration, setDuration] = useState<BanDuration>("permanent");
  const [customUntil, setCustomUntil] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const durations: { value: BanDuration; label: string }[] = [
    { value: "permanent", label: "Permanent" },
    { value: "1d", label: "1 Day" },
    { value: "7d", label: "7 Days" },
    { value: "30d", label: "30 Days" },
    { value: "90d", label: "90 Days" },
    { value: "custom", label: "Custom" },
  ];

  const handleSubmit = async () => {
    if (!reason.trim()) {
      setError("Ban reason is required.");
      return;
    }
    if (duration === "custom" && !customUntil) {
      setError("Please select a custom end date.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await onBan(
        reason.trim(),
        duration,
        duration === "custom" ? customUntil : undefined
      );
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to ban user"
      );
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[55] flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      {/* Modal */}
      <div className="relative z-10 w-full max-w-md mx-4 rounded-xl bg-white shadow-2xl">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-start gap-3 mb-5">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-100">
              <Ban className="h-5 w-5 text-red-600" />
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-text-dark">
                Ban User
              </h3>
              <p className="mt-0.5 text-sm text-text-muted">
                {user.name} {user.surname} ({user.email})
              </p>
            </div>
          </div>

          {/* Reason */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-text-dark mb-1.5">
              Reason <span className="text-red-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Describe the reason for banning this user..."
              rows={3}
              className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-text-dark placeholder-text-muted outline-none transition-colors focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20 resize-none"
            />
          </div>

          {/* Duration */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-text-dark mb-2">
              Duration
            </label>
            <div className="grid grid-cols-3 gap-2">
              {durations.map((d) => (
                <button
                  key={d.value}
                  onClick={() => setDuration(d.value)}
                  className={`rounded-lg border px-3 py-2 text-xs font-medium transition-colors ${
                    duration === d.value
                      ? "border-red-300 bg-red-50 text-red-700"
                      : "border-gray-200 bg-white text-text-muted hover:border-gray-300 hover:text-text-dark"
                  }`}
                >
                  {d.label}
                </button>
              ))}
            </div>
          </div>

          {/* Custom date picker */}
          {duration === "custom" && (
            <div className="mb-4">
              <label className="block text-sm font-medium text-text-dark mb-1.5">
                Ban Until
              </label>
              <input
                type="datetime-local"
                value={customUntil}
                onChange={(e) => setCustomUntil(e.target.value)}
                min={new Date().toISOString().slice(0, 16)}
                className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-text-dark outline-none transition-colors focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
              />
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              <AlertTriangle className="h-4 w-4 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-text-dark transition-colors hover:bg-gray-50 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:opacity-50"
            >
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              Ban User
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Unban Confirmation Modal ─────────────────────────────────────────────

function UnbanModal({
  user,
  onClose,
  onUnban,
}: {
  user: UserDetail;
  onClose: () => void;
  onUnban: () => Promise<void>;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleUnban = async () => {
    setLoading(true);
    setError(null);
    try {
      await onUnban();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to unban user"
      );
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[55] flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      {/* Modal */}
      <div className="relative z-10 w-full max-w-sm mx-4 rounded-xl bg-white shadow-2xl">
        <div className="p-6">
          <div className="flex items-start gap-3 mb-5">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100">
              <ShieldCheck className="h-5 w-5 text-emerald-600" />
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-text-dark">
                Unban User
              </h3>
              <p className="mt-0.5 text-sm text-text-muted">
                Are you sure you want to unban{" "}
                <span className="font-medium text-text-dark">
                  {user.name} {user.surname}
                </span>
                ?
              </p>
            </div>
          </div>

          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              <AlertTriangle className="h-4 w-4 flex-shrink-0" />
              {error}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-text-dark transition-colors hover:bg-gray-50 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              onClick={handleUnban}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
            >
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              Unban User
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── User Detail Panel ────────────────────────────────────────────────────

function UserDetailPanel({
  userId,
  onClose,
  onBanSuccess,
}: {
  userId: string;
  onClose: () => void;
  onBanSuccess: (message: string) => void;
}) {
  const { token } = useAuth();
  const [user, setUser] = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showBanModal, setShowBanModal] = useState(false);
  const [showUnbanModal, setShowUnbanModal] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  const fetchUserDetail = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await adminAPI.userDetail(token, userId);
      setUser(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load user details"
      );
    } finally {
      setLoading(false);
    }
  }, [token, userId]);

  useEffect(() => {
    fetchUserDetail();
  }, [fetchUserDetail]);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  // Prevent body scroll when panel is open
  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "";
    };
  }, []);

  const handleBan = async (
    reason: string,
    duration: string,
    customUntil?: string
  ) => {
    if (!token) return;
    await adminAPI.banUser(token, userId, {
      reason,
      duration,
      custom_until: customUntil,
    });
    setShowBanModal(false);
    await fetchUserDetail();
    onBanSuccess(`${user?.name} ${user?.surname} has been banned.`);
  };

  const handleUnban = async () => {
    if (!token) return;
    await adminAPI.unbanUser(token, userId);
    setShowUnbanModal(false);
    await fetchUserDetail();
    onBanSuccess(`${user?.name} ${user?.surname} has been unbanned.`);
  };

  return (
    <>
      <div className="fixed inset-0 z-40 flex justify-end">
        {/* Dark overlay backdrop */}
        <div
          className="absolute inset-0 bg-black/40 backdrop-blur-[2px] transition-opacity duration-300"
          onClick={onClose}
        />

        {/* Slide-out panel */}
        <div
          ref={panelRef}
          className="relative z-10 w-full max-w-[480px] bg-white shadow-2xl transform transition-transform duration-300 ease-out animate-in slide-in-from-right"
          style={{ height: "100vh" }}
        >
          {/* Header */}
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-gray-100 bg-white px-6 py-4">
            <h2 className="text-lg font-semibold text-text-dark">
              User Details
            </h2>
            <button
              onClick={onClose}
              className="flex h-8 w-8 items-center justify-center rounded-lg text-text-muted transition-colors hover:bg-gray-100 hover:text-text-dark"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Content */}
          <div className="overflow-y-auto" style={{ height: "calc(100vh - 65px)" }}>
            {loading && (
              <div className="flex items-center justify-center py-20">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            )}

            {error && (
              <div className="flex flex-col items-center justify-center gap-3 px-6 py-20">
                <AlertTriangle className="h-8 w-8 text-red-400" />
                <p className="text-sm text-red-600 text-center">{error}</p>
                <button
                  onClick={fetchUserDetail}
                  className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark transition"
                >
                  Retry
                </button>
              </div>
            )}

            {user && !loading && (
              <div className="px-6 py-5 space-y-6">
                {/* Profile Header */}
                <div className="flex items-start gap-4">
                  <div className="flex h-14 w-14 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                    <UserIcon className="h-7 w-7" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-xl font-bold text-text-dark truncate">
                      {user.name} {user.surname}
                    </h3>
                    <div className="mt-1 flex items-center gap-2 flex-wrap">
                      <StatusBadge
                        status={user.role}
                        variant={
                          user.role === "driver" ? "info" : "neutral"
                        }
                      />
                      {user.is_banned ? (
                        <StatusBadge status="Banned" variant="danger" />
                      ) : (
                        <StatusBadge
                          status={user.is_active ? "Active" : "Inactive"}
                          variant={user.is_active ? "success" : "danger"}
                        />
                      )}
                      {user.role === "driver" && (
                        <StatusBadge
                          status={user.driver_online ? "Online" : "Offline"}
                          variant={user.driver_online ? "success" : "neutral"}
                        />
                      )}
                    </div>
                  </div>
                </div>

                {/* Ban Info */}
                {user.is_banned && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-4">
                    <div className="flex items-center gap-2 mb-2">
                      <Ban className="h-4 w-4 text-red-600" />
                      <span className="text-sm font-semibold text-red-700">
                        Account Banned
                      </span>
                    </div>
                    {user.ban_reason && (
                      <p className="text-sm text-red-600 mb-1">
                        <span className="font-medium">Reason:</span>{" "}
                        {user.ban_reason}
                      </p>
                    )}
                    {user.banned_until && (
                      <p className="text-sm text-red-600">
                        <span className="font-medium">Until:</span>{" "}
                        {formatDateTime(user.banned_until)}
                      </p>
                    )}
                    {!user.banned_until && (
                      <p className="text-sm text-red-600 font-medium">
                        Permanent ban
                      </p>
                    )}
                  </div>
                )}

                {/* Contact Info */}
                <div className="space-y-3">
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                    Contact
                  </h4>
                  <div className="space-y-2">
                    <div className="flex items-center gap-3 text-sm">
                      <Mail className="h-4 w-4 text-text-muted flex-shrink-0" />
                      <span className="text-text-dark truncate">
                        {user.email}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-sm">
                      <Phone className="h-4 w-4 text-text-muted flex-shrink-0" />
                      <span className="text-text-dark">
                        {user.phone_number || "---"}
                      </span>
                    </div>
                    {(user.city || user.province) && (
                      <div className="flex items-center gap-3 text-sm">
                        <MapPin className="h-4 w-4 text-text-muted flex-shrink-0" />
                        <span className="text-text-dark">
                          {[user.city, user.province]
                            .filter(Boolean)
                            .join(", ") || "---"}
                        </span>
                      </div>
                    )}
                    <div className="flex items-center gap-3 text-sm">
                      <Calendar className="h-4 w-4 text-text-muted flex-shrink-0" />
                      <span className="text-text-dark">
                        Joined {formatDate(user.date_joined)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Rating */}
                <div className="space-y-2">
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                    Rating
                  </h4>
                  {user.rating > 0 ? (
                    <StarRating
                      rating={user.rating}
                      count={user.rating_count}
                    />
                  ) : (
                    <span className="text-sm text-text-muted">No ratings yet</span>
                  )}
                </div>

                {/* Delivery count */}
                <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10">
                    <Users className="h-4 w-4 text-primary" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-text-dark">
                      {user.delivery_count} Deliveries
                    </p>
                    <p className="text-xs text-text-muted">Total completed</p>
                  </div>
                </div>

                {/* Driver-specific info */}
                {user.role === "driver" && (
                  <>
                    {/* Vehicle info */}
                    {user.driver_vehicle && (
                      <div className="space-y-3">
                        <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                          <Car className="h-3.5 w-3.5" />
                          Vehicle
                        </h4>
                        <div className="rounded-lg border border-gray-100 bg-gray-50/50 p-3 space-y-1.5">
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">Type</span>
                            <span className="font-medium text-text-dark capitalize">
                              {user.driver_vehicle.delivery_vehicle}
                            </span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">Name</span>
                            <span className="font-medium text-text-dark">
                              {user.driver_vehicle.car_name || "---"}
                            </span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">Plate</span>
                            <span className="font-medium text-text-dark">
                              {user.driver_vehicle.number_plate || "---"}
                            </span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">Color</span>
                            <span className="font-medium text-text-dark capitalize">
                              {user.driver_vehicle.color || "---"}
                            </span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Finances */}
                    {user.driver_finances && (
                      <div className="space-y-3">
                        <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                          <DollarSign className="h-3.5 w-3.5" />
                          Finances
                        </h4>
                        <div className="grid grid-cols-2 gap-2">
                          <div className="rounded-lg bg-emerald-50 p-3">
                            <p className="text-xs text-emerald-600 font-medium">
                              Earnings
                            </p>
                            <p className="text-lg font-bold text-emerald-700">
                              {formatCurrency(user.driver_finances.earnings)}
                            </p>
                          </div>
                          <div className="rounded-lg bg-red-50 p-3">
                            <p className="text-xs text-red-600 font-medium">
                              Charges
                            </p>
                            <p className="text-lg font-bold text-red-700">
                              {formatCurrency(user.driver_finances.charges)}
                            </p>
                          </div>
                          <div className="rounded-lg bg-blue-50 p-3">
                            <p className="text-xs text-blue-600 font-medium">
                              Profit
                            </p>
                            <p className="text-lg font-bold text-blue-700">
                              {formatCurrency(user.driver_finances.profit)}
                            </p>
                          </div>
                          <div className="rounded-lg bg-amber-50 p-3">
                            <p className="text-xs text-amber-600 font-medium">
                              Trips
                            </p>
                            <p className="text-lg font-bold text-amber-700">
                              {user.driver_finances.total_trips}
                            </p>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Balance */}
                    {user.driver_balance && (
                      <div className="space-y-3">
                        <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                          Balance
                        </h4>
                        <div className="rounded-lg border border-gray-100 bg-gray-50/50 p-3 space-y-1.5">
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">
                              Amount Owed
                            </span>
                            <span className="font-medium text-red-600">
                              {formatCurrency(
                                user.driver_balance.owed_amount
                              )}
                            </span>
                          </div>
                          <div className="flex justify-between text-sm">
                            <span className="text-text-muted">
                              Total Paid
                            </span>
                            <span className="font-medium text-emerald-600">
                              {formatCurrency(
                                user.driver_balance.total_paid
                              )}
                            </span>
                          </div>
                        </div>
                      </div>
                    )}
                  </>
                )}

                {/* Recent Deliveries */}
                {user.recent_deliveries.length > 0 && (
                  <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                      Recent Deliveries
                    </h4>
                    <div className="rounded-lg border border-gray-100 overflow-hidden">
                      <table className="w-full">
                        <thead>
                          <tr className="bg-gray-50/80">
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              ID
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Fare
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Status
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Date
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {user.recent_deliveries.map((d) => (
                            <tr
                              key={d.delivery_id}
                              className="border-t border-gray-50"
                            >
                              <td className="px-3 py-2 text-xs text-text-dark font-mono">
                                {d.delivery_id.slice(0, 8)}...
                              </td>
                              <td className="px-3 py-2 text-xs text-text-dark">
                                {formatCurrency(d.fare)}
                              </td>
                              <td className="px-3 py-2">
                                <StatusBadge
                                  status={
                                    d.successful ? "Successful" : "Failed"
                                  }
                                  variant={
                                    d.successful ? "success" : "danger"
                                  }
                                />
                              </td>
                              <td className="px-3 py-2 text-xs text-text-muted">
                                {formatDate(d.date)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* Recent Payments */}
                {user.recent_payments.length > 0 && (
                  <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted">
                      Recent Payments
                    </h4>
                    <div className="rounded-lg border border-gray-100 overflow-hidden">
                      <table className="w-full">
                        <thead>
                          <tr className="bg-gray-50/80">
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Amount
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Method
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Status
                            </th>
                            <th className="px-3 py-2 text-left text-xs font-semibold text-text-muted">
                              Date
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {user.recent_payments.map((p) => (
                            <tr
                              key={p.id}
                              className="border-t border-gray-50"
                            >
                              <td className="px-3 py-2 text-xs text-text-dark font-medium">
                                {formatCurrency(p.amount)}
                              </td>
                              <td className="px-3 py-2 text-xs text-text-dark capitalize">
                                {p.payment_method}
                              </td>
                              <td className="px-3 py-2">
                                <StatusBadge status={p.status} />
                              </td>
                              <td className="px-3 py-2 text-xs text-text-muted">
                                {formatDate(p.created_at)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="sticky bottom-0 bg-white border-t border-gray-100 -mx-6 px-6 py-4 mt-4">
                  {user.is_banned ? (
                    <button
                      onClick={() => setShowUnbanModal(true)}
                      className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
                    >
                      <ShieldCheck className="h-4 w-4" />
                      Unban User
                    </button>
                  ) : (
                    <button
                      onClick={() => setShowBanModal(true)}
                      className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-red-700"
                    >
                      <Ban className="h-4 w-4" />
                      Ban User
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Ban / Unban modals */}
      {showBanModal && user && (
        <BanModal
          user={user}
          onClose={() => setShowBanModal(false)}
          onBan={handleBan}
        />
      )}
      {showUnbanModal && user && (
        <UnbanModal
          user={user}
          onClose={() => setShowUnbanModal(false)}
          onUnban={handleUnban}
        />
      )}
    </>
  );
}

// ── Static column definitions ────────────────────────────────────────────

const topSendersColumns = [
  {
    key: "_index",
    header: "Rank",
    render: (row: Row) => (
      <span className="font-semibold text-text-muted">
        {(row._index as number) + 1}
      </span>
    ),
  },
  {
    key: "name",
    header: "Name",
    sortable: true,
    render: (row: Row) => (
      <span className="font-medium text-text-dark">{row.name as string}</span>
    ),
  },
  {
    key: "email",
    header: "Email",
    render: (row: Row) => (
      <span className="text-text-muted">{row.email as string}</span>
    ),
  },
  {
    key: "delivery_count",
    header: "Deliveries",
    sortable: true,
    render: (row: Row) => (
      <span className="font-semibold">
        {(row.delivery_count as number).toLocaleString()}
      </span>
    ),
  },
];

// ── Page ─────────────────────────────────────────────────────────────────

export default function UsersPage() {
  const { token } = useAuth();

  // Metrics state
  const [metrics, setMetrics] = useState<UserMetrics | null>(null);
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [metricsError, setMetricsError] = useState<string | null>(null);

  // Users list state
  const [usersList, setUsersList] = useState<UsersListResponse | null>(null);
  const [usersLoading, setUsersLoading] = useState(true);
  const [usersPage, setUsersPage] = useState(1);
  const [usersSearch, setUsersSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<"all" | "user" | "driver">(
    "user"
  );

  // Detail panel state
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  // Toast state
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // ── Fetch metrics ────────────────────────────────────────────────────

  const fetchMetrics = useCallback(async () => {
    if (!token) return;
    setMetricsLoading(true);
    setMetricsError(null);
    try {
      const data = await adminAPI.userMetrics(token);
      setMetrics(data);
    } catch (err) {
      setMetricsError(
        err instanceof Error ? err.message : "Failed to load user metrics"
      );
    } finally {
      setMetricsLoading(false);
    }
  }, [token]);

  // ── Fetch users list ─────────────────────────────────────────────────

  const fetchUsers = useCallback(async () => {
    if (!token) return;
    setUsersLoading(true);
    try {
      const params = new URLSearchParams();
      params.set("page", String(usersPage));
      params.set("page_size", "15");
      if (usersSearch) params.set("search", usersSearch);
      if (roleFilter !== "all") params.set("role", roleFilter);

      const data = await adminAPI.usersList(token, params.toString());
      setUsersList(data);
    } catch {
      // Silently handle list errors (metrics error already shown above)
    } finally {
      setUsersLoading(false);
    }
  }, [token, usersPage, usersSearch, roleFilter]);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Debounce search — reset to page 1 when searching
  const handleSearch = useCallback((query: string) => {
    setUsersSearch(query);
    setUsersPage(1);
  }, []);

  const handleRoleFilter = useCallback(
    (role: "all" | "user" | "driver") => {
      setRoleFilter(role);
      setUsersPage(1);
    },
    []
  );

  const handleBanSuccess = useCallback(
    (message: string) => {
      setToastMessage(message);
      // Refresh users list to reflect ban/unban status
      fetchUsers();
    },
    [fetchUsers]
  );

  // ── Derived: new signups this month ──────────────────────────────────

  const newThisMonth = useMemo(() => {
    if (!metrics?.signups_over_time) return 0;
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();
    return metrics.signups_over_time
      .filter((p) => {
        const d = new Date(p.date);
        return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
      })
      .reduce((sum, p) => sum + p.count, 0);
  }, [metrics]);

  // ── Top senders data ─────────────────────────────────────────────────

  const topSendersData: Row[] = useMemo(
    () =>
      (metrics?.top_senders ?? []).map((s, i) => ({
        ...s,
        _index: i,
      })),
    [metrics]
  );

  // ── All Users columns (with ban status + actions) ─────────────────────

  const allUsersColumns = useMemo(
    () => [
      {
        key: "name",
        header: "Name",
        sortable: true,
        render: (row: Row) => (
          <span className="font-medium text-text-dark">
            {row.name as string} {row.surname as string}
          </span>
        ),
      },
      {
        key: "email",
        header: "Email",
        render: (row: Row) => (
          <span className="text-text-muted">{row.email as string}</span>
        ),
      },
      {
        key: "phone_number",
        header: "Phone",
        render: (row: Row) => (
          <span className="text-text-muted">
            {(row.phone_number as string) || "---"}
          </span>
        ),
      },
      {
        key: "role",
        header: "Role",
        render: (row: Row) => (
          <StatusBadge
            status={row.role as string}
            variant={(row.role as string) === "driver" ? "info" : "neutral"}
          />
        ),
      },
      {
        key: "date_joined",
        header: "Joined",
        sortable: true,
        render: (row: Row) => (
          <span className="text-text-muted text-xs">
            {formatDate(row.date_joined as string)}
          </span>
        ),
      },
      {
        key: "is_active",
        header: "Status",
        render: (row: Row) => (
          <StatusBadge
            status={row.is_active ? "Active" : "Inactive"}
            variant={row.is_active ? "success" : "danger"}
          />
        ),
      },
      {
        key: "is_banned",
        header: "Ban Status",
        render: (row: Row) => {
          const isBanned = row.is_banned as boolean;
          const bannedUntil = row.banned_until as string | null | undefined;
          if (isBanned) {
            return (
              <span className="group relative">
                <StatusBadge status="Banned" variant="danger" />
                {bannedUntil && (
                  <span className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 w-max rounded-md bg-gray-900 px-2 py-1 text-xs text-white opacity-0 group-hover:opacity-100 transition-opacity shadow-lg z-20">
                    Until {formatDateTime(bannedUntil)}
                  </span>
                )}
              </span>
            );
          }
          return <StatusBadge status="Active" variant="success" />;
        },
      },
      {
        key: "rating",
        header: "Rating",
        sortable: true,
        render: (row: Row) => {
          const rating = row.rating as number | null | undefined;
          if (rating == null) {
            return <span className="text-text-muted">---</span>;
          }
          return (
            <span className="inline-flex items-center gap-1">
              <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
              <span className="text-sm">{rating.toFixed(1)}</span>
            </span>
          );
        },
      },
      {
        key: "actions",
        header: "Actions",
        render: (row: Row) => (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setSelectedUserId(row.id as string);
            }}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-2.5 py-1.5 text-xs font-medium text-text-dark transition-colors hover:bg-gray-50 hover:border-gray-300"
          >
            <Eye className="h-3.5 w-3.5" />
            View
          </button>
        ),
      },
    ],
    []
  );

  // ── Loading state ────────────────────────────────────────────────────

  if (metricsLoading) {
    return <LoadingSpinner text="Loading user analytics..." size="lg" />;
  }

  // ── Error state ──────────────────────────────────────────────────────

  if (metricsError) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-sm text-red-600">{metricsError}</p>
        <button
          onClick={fetchMetrics}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark transition"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!metrics) return null;

  // ── Render ───────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Success toast */}
      {toastMessage && (
        <SuccessToast
          message={toastMessage}
          onClose={() => setToastMessage(null)}
        />
      )}

      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-text-dark">User Analytics</h1>
        <p className="mt-1 text-sm text-text-muted">
          Insights into user activity, growth, and engagement.
        </p>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="Total Users"
          value={metrics.total_users.toLocaleString()}
          icon={<Users className="h-5 w-5" />}
        />
        <StatsCard
          title="Active Users"
          value={metrics.active_users.toLocaleString()}
          icon={<UserCheck className="h-5 w-5" />}
          subtitle="Last 30 days"
        />
        <StatsCard
          title="Repeat Users"
          value={metrics.repeat_users.toLocaleString()}
          icon={<Repeat className="h-5 w-5" />}
          subtitle="2+ deliveries"
        />
        <StatsCard
          title="New This Month"
          value={newThisMonth.toLocaleString()}
          icon={<UserPlus className="h-5 w-5" />}
        />
      </div>

      {/* Signups Over Time chart */}
      <ChartCard
        title="Signups Over Time"
        subtitle="New user registrations over the last 30 days"
      >
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={metrics.signups_over_time}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis
              dataKey="date"
              tickFormatter={formatChartDate}
              tick={{ fontSize: 12, fill: "#6b7280" }}
              axisLine={{ stroke: "#e5e7eb" }}
              tickLine={false}
            />
            <YAxis
              allowDecimals={false}
              tick={{ fontSize: 12, fill: "#6b7280" }}
              axisLine={{ stroke: "#e5e7eb" }}
              tickLine={false}
            />
            <Tooltip
              labelFormatter={formatChartDate}
              contentStyle={{
                borderRadius: "8px",
                border: "1px solid #e5e7eb",
                boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1)",
              }}
            />
            <Line
              type="monotone"
              dataKey="count"
              name="Signups"
              stroke={CHART_COLORS[0]}
              strokeWidth={2.5}
              dot={{ r: 3, fill: CHART_COLORS[0] }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </ChartCard>

      {/* Top Senders */}
      <div>
        <h2 className="text-lg font-semibold text-text-dark mb-3">
          Top Senders
        </h2>
        <DataTable columns={topSendersColumns} data={topSendersData} />
      </div>

      {/* All Users table */}
      <div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-3">
          <h2 className="text-lg font-semibold text-text-dark">All Users</h2>

          {/* Role filter tabs */}
          <div className="flex items-center gap-1 rounded-lg bg-gray-100 p-1">
            {(["all", "user", "driver"] as const).map((role) => (
              <button
                key={role}
                onClick={() => handleRoleFilter(role)}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors capitalize ${
                  roleFilter === role
                    ? "bg-white text-text-dark shadow-sm"
                    : "text-text-muted hover:text-text-dark"
                }`}
              >
                {role === "all" ? "All Roles" : `${role}s`}
              </button>
            ))}
          </div>
        </div>

        <DataTable
          columns={allUsersColumns}
          data={usersList?.results ?? []}
          loading={usersLoading}
          onSearch={handleSearch}
          searchPlaceholder="Search by name or email..."
          pagination={
            usersList
              ? {
                  page: usersList.page,
                  totalPages: usersList.total_pages,
                  total: usersList.total,
                  onPageChange: setUsersPage,
                }
              : undefined
          }
        />
      </div>

      {/* User Detail Slide-Out Panel */}
      {selectedUserId && (
        <UserDetailPanel
          userId={selectedUserId}
          onClose={() => setSelectedUserId(null)}
          onBanSuccess={handleBanSuccess}
        />
      )}
    </div>
  );
}
