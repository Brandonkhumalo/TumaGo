"use client";

import React, { useEffect, useState, useCallback } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";
import {
  StatsCard,
  DataTable,
  LoadingSpinner,
} from "@/components/dashboard";
import {
  Users,
  Wifi,
  CheckCircle,
  Star,
  TrendingUp,
  UserX,
} from "lucide-react";

// ── Types ────────────────────────────────────────────────────────────────

interface LeaderboardEntry {
  name: string;
  email: string;
  total_trips: number;
  earnings: number;
  rating: number;
}

interface DriverMetrics {
  total_drivers: number;
  online_drivers: number;
  available_drivers: number;
  avg_rating: number;
  leaderboard: LeaderboardEntry[];
  acceptance_rate: number;
  drivers_with_no_recent_activity: {
    count: number;
    sample: { id: string; name: string; surname: string; email: string }[];
  };
}

// ── Helpers ──────────────────────────────────────────────────────────────

/** Shorthand alias to keep column definitions concise */
type Row = Record<string, unknown>;

function formatCurrency(value: number): string {
  return `$${value.toFixed(2)}`;
}

/** Medal styling for leaderboard top 3 rows */
const MEDAL_STYLES: Record<number, { bg: string; text: string; icon: string }> = {
  0: { bg: "bg-amber-50", text: "text-amber-700", icon: "1" },
  1: { bg: "bg-gray-100", text: "text-gray-500", icon: "2" },
  2: { bg: "bg-orange-50", text: "text-orange-700", icon: "3" },
};

// ── Leaderboard columns (static, avoids recreating on every render) ────

const leaderboardColumns = [
  {
    key: "_index",
    header: "#",
    render: (row: Row) => {
      const index = row._index as number;
      const medal = MEDAL_STYLES[index];
      if (medal) {
        return (
          <span
            className={`inline-flex items-center justify-center w-7 h-7 rounded-full ${medal.bg} ${medal.text} text-sm font-bold`}
          >
            {medal.icon}
          </span>
        );
      }
      return <span className="text-text-muted font-medium">{index + 1}</span>;
    },
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
    key: "total_trips",
    header: "Total Trips",
    sortable: true,
    render: (row: Row) => (
      <span className="font-semibold">
        {((row.total_trips as number) ?? 0).toLocaleString()}
      </span>
    ),
  },
  {
    key: "earnings",
    header: "Earnings ($)",
    sortable: true,
    render: (row: Row) => (
      <span className="font-semibold text-emerald-700">
        {formatCurrency((row.earnings as number) ?? 0)}
      </span>
    ),
  },
  {
    key: "rating",
    header: "Rating",
    sortable: true,
    render: (row: Row) => (
      <span className="inline-flex items-center gap-1">
        <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
        <span className="font-semibold">{((row.rating as number) ?? 0).toFixed(1)}</span>
      </span>
    ),
  },
];

// ── Page ─────────────────────────────────────────────────────────────────

export default function DriversPage() {
  const { token } = useAuth();
  const [metrics, setMetrics] = useState<DriverMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await adminAPI.driverMetrics(token);
      setMetrics(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load driver metrics"
      );
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // ── Loading state ────────────────────────────────────────────────────

  if (loading) {
    return <LoadingSpinner text="Loading driver analytics..." size="lg" />;
  }

  // ── Error state ──────────────────────────────────────────────────────

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-sm text-red-600">{error}</p>
        <button
          onClick={fetchData}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark transition"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!metrics) return null;

  // Enrich leaderboard data with an _index field so the rank column can
  // display the correct position number.
  const leaderboardData: Row[] = metrics.leaderboard.map((entry, i) => ({
    ...entry,
    _index: i,
  }));

  // ── Render ───────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-text-dark">Driver Analytics</h1>
        <p className="mt-1 text-sm text-text-muted">
          Real-time overview of driver activity and performance.
        </p>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        <StatsCard
          title="Total Drivers"
          value={metrics.total_drivers.toLocaleString()}
          icon={<Users className="h-5 w-5" />}
        />
        <StatsCard
          title="Online Now"
          value={metrics.online_drivers.toLocaleString()}
          icon={<Wifi className="h-5 w-5" />}
          subtitle={`${metrics.total_drivers > 0 ? ((metrics.online_drivers / metrics.total_drivers) * 100).toFixed(1) : 0}% of total`}
        />
        <StatsCard
          title="Available"
          value={metrics.available_drivers.toLocaleString()}
          icon={<CheckCircle className="h-5 w-5" />}
          subtitle="Ready for trips"
        />
        <StatsCard
          title="Avg Rating"
          value={`${metrics.avg_rating.toFixed(1)} ★`}
          icon={<Star className="h-5 w-5" />}
        />
        <StatsCard
          title="Acceptance Rate"
          value={`${metrics.acceptance_rate.toFixed(1)}%`}
          icon={<TrendingUp className="h-5 w-5" />}
        />
      </div>

      {/* Inactive drivers card */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatsCard
          title="Inactive Drivers"
          value={metrics.drivers_with_no_recent_activity.count.toLocaleString()}
          icon={<UserX className="h-5 w-5" />}
          subtitle="No activity in 7+ days"
          changeType={
            metrics.drivers_with_no_recent_activity.count > 0 ? "negative" : "positive"
          }
          change={
            metrics.drivers_with_no_recent_activity.count > 0
              ? `${((metrics.drivers_with_no_recent_activity.count / metrics.total_drivers) * 100).toFixed(1)}% churn risk`
              : "All drivers active"
          }
        />
      </div>

      {/* Driver Leaderboard */}
      <div>
        <h2 className="text-lg font-semibold text-text-dark mb-3">
          Driver Leaderboard
        </h2>
        <DataTable columns={leaderboardColumns} data={leaderboardData} />
      </div>
    </div>
  );
}
