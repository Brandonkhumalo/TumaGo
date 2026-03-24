"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";
import {
  StatsCard,
  DataTable,
  ChartCard,
  LoadingSpinner,
  StatusBadge,
} from "@/components/dashboard";
import {
  Users,
  Truck,
  Package,
  DollarSign,
  Wifi,
  UserCheck,
  Clock,
  TrendingUp,
  AlertCircle,
  RefreshCw,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";

// ── Types ────────────────────────────────────────────────────────────────

interface OverviewData {
  total_users: number;
  total_drivers: number;
  drivers_online: number;
  drivers_available: number;
  total_deliveries: number;
  successful_deliveries: number;
  cancelled_deliveries: number;
  pending_trip_requests: number;
  total_revenue: number;
  today_deliveries: number;
  today_revenue: number;
}

interface DeliveryMetrics {
  total: number;
  successful: number;
  cancelled: number;
  completion_rate: number;
  avg_fare: number;
  by_vehicle: { name: string; value: number }[];
  by_hour: { hour: number; count: number }[];
  by_payment_method: { name: string; value: number }[];
  daily_trend: { date: string; count: number; revenue: number }[];
}

interface DeliveryRecord {
  id: string;
  driver_name: string;
  client_name: string;
  fare: number;
  payment_method: string;
  status: string;
  created_at: string;
  [key: string]: unknown;
}

// ── Constants ────────────────────────────────────────────────────────────

const CHART_COLORS = [
  "#00a4e4",
  "#0e74bc",
  "#FF6B35",
  "#10b981",
  "#8b5cf6",
  "#f59e0b",
];

const REFRESH_INTERVAL = 30_000; // 30 seconds

// ── Helpers ──────────────────────────────────────────────────────────────

function formatCurrency(value: number): string {
  return `$${value.toFixed(2)}`;
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function formatShortDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

function truncateId(id: string): string {
  if (!id) return "--";
  return id.length > 8 ? `${id.slice(0, 8)}...` : id;
}

// ── Custom Tooltip ───────────────────────────────────────────────────────

function BarChartTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: { value: number }[];
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg bg-white px-3 py-2 shadow-lg border border-gray-100 text-sm">
      <p className="font-medium text-text-dark">{label}</p>
      <p className="text-text-muted">
        {payload[0].value.toLocaleString()} deliveries
      </p>
    </div>
  );
}

function PieChartTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { name: string; value: number }[];
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg bg-white px-3 py-2 shadow-lg border border-gray-100 text-sm">
      <p className="font-medium text-text-dark">{payload[0].name}</p>
      <p className="text-text-muted">
        {formatCurrency(payload[0].value)}
      </p>
    </div>
  );
}

// ── Recent Deliveries Table Columns ──────────────────────────────────────

const recentDeliveriesColumns = [
  {
    key: "id",
    header: "ID",
    render: (item: DeliveryRecord) => (
      <span className="font-mono text-xs text-text-muted">
        {truncateId(item.id)}
      </span>
    ),
  },
  {
    key: "driver_name",
    header: "Driver",
  },
  {
    key: "client_name",
    header: "Client",
  },
  {
    key: "fare",
    header: "Fare",
    render: (item: DeliveryRecord) => formatCurrency(item.fare ?? 0),
  },
  {
    key: "status",
    header: "Status",
    render: (item: DeliveryRecord) => <StatusBadge status={item.status ?? "unknown"} />,
  },
  {
    key: "created_at",
    header: "Date",
    render: (item: DeliveryRecord) =>
      item.created_at ? formatDate(item.created_at) : "--",
  },
];

// ── Dashboard Page ───────────────────────────────────────────────────────

export default function DashboardPage() {
  const { token } = useAuth();

  const [overview, setOverview] = useState<OverviewData | null>(null);
  const [metrics, setMetrics] = useState<DeliveryMetrics | null>(null);
  const [recentDeliveries, setRecentDeliveries] = useState<DeliveryRecord[]>(
    []
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchData = useCallback(
    async (showLoading = false) => {
      if (!token) return;

      if (showLoading) setLoading(true);
      setError(null);

      try {
        const [overviewRes, metricsRes, deliveriesRes] = await Promise.all([
          adminAPI.overview(token),
          adminAPI.deliveryMetrics(token, "period=week"),
          adminAPI.deliveriesList(token, "page_size=5"),
        ]);

        setOverview(overviewRes);
        setMetrics(metricsRes);
        setRecentDeliveries(deliveriesRes.results ?? []);
        setLastUpdated(new Date());
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Failed to load dashboard data"
        );
      } finally {
        setLoading(false);
      }
    },
    [token]
  );

  // Initial fetch + auto-refresh every 30s
  useEffect(() => {
    fetchData(true);

    intervalRef.current = setInterval(() => fetchData(false), REFRESH_INTERVAL);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [fetchData]);

  // ── Loading state ────────────────────────────────────────────────────
  if (loading && !overview) {
    return <LoadingSpinner text="Loading dashboard..." size="lg" />;
  }

  // ── Error state ──────────────────────────────────────────────────────
  if (error && !overview) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-16">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-500">
          <AlertCircle className="h-7 w-7" />
        </div>
        <h3 className="text-base font-semibold text-text-dark">
          Failed to load dashboard
        </h3>
        <p className="text-sm text-text-muted max-w-md text-center">{error}</p>
        <button
          onClick={() => fetchData(true)}
          className="mt-2 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition hover:bg-primary-dark"
        >
          <RefreshCw className="h-4 w-4" />
          Retry
        </button>
      </div>
    );
  }

  // Prepare chart data
  const dailyTrendData = (metrics?.daily_trend ?? []).map((d) => ({
    ...d,
    label: formatShortDate(d.date),
  }));

  const paymentMethodData = metrics?.by_payment_method ?? [];

  const completionRate =
    overview && overview.total_deliveries > 0
      ? ((overview.successful_deliveries / overview.total_deliveries) * 100).toFixed(1)
      : "0";

  // ── Render ───────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-dark">
            Dashboard Overview
          </h1>
          {lastUpdated && (
            <p className="text-xs text-text-muted mt-1">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </p>
          )}
        </div>
        <button
          onClick={() => fetchData(false)}
          className="inline-flex items-center gap-2 self-start rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-medium text-text-dark transition hover:bg-gray-50"
        >
          <RefreshCw className="h-4 w-4" />
          Refresh
        </button>
      </div>

      {/* Error banner (non-blocking, when data exists but refresh fails) */}
      {error && overview && (
        <div className="flex items-center gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error} -- Showing cached data.</span>
        </div>
      )}

      {/* Row 1: Primary stats */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="Total Users"
          value={(overview?.total_users ?? 0).toLocaleString()}
          icon={<Users className="h-5 w-5" />}
          subtitle="Registered users"
        />
        <StatsCard
          title="Total Drivers"
          value={(overview?.total_drivers ?? 0).toLocaleString()}
          icon={<Truck className="h-5 w-5" />}
          subtitle="Registered drivers"
        />
        <StatsCard
          title="Active Deliveries"
          value={(overview?.today_deliveries ?? 0).toLocaleString()}
          icon={<Package className="h-5 w-5" />}
          subtitle="Today"
        />
        <StatsCard
          title="Revenue Today"
          value={formatCurrency(overview?.today_revenue ?? 0)}
          icon={<DollarSign className="h-5 w-5" />}
          subtitle="Gross revenue"
        />
      </div>

      {/* Row 2: Secondary stats */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="Drivers Online"
          value={(overview?.drivers_online ?? 0).toLocaleString()}
          icon={<Wifi className="h-5 w-5" />}
          changeType={
            (overview?.drivers_online ?? 0) > 0 ? "positive" : "neutral"
          }
          change={
            (overview?.drivers_online ?? 0) > 0 ? "Active" : "None"
          }
        />
        <StatsCard
          title="Drivers Available"
          value={(overview?.drivers_available ?? 0).toLocaleString()}
          icon={<UserCheck className="h-5 w-5" />}
          subtitle="Ready for trips"
        />
        <StatsCard
          title="Pending Requests"
          value={(overview?.pending_trip_requests ?? 0).toLocaleString()}
          icon={<Clock className="h-5 w-5" />}
          changeType={
            (overview?.pending_trip_requests ?? 0) > 5 ? "negative" : "neutral"
          }
          change={
            (overview?.pending_trip_requests ?? 0) > 5 ? "High" : undefined
          }
        />
        <StatsCard
          title="Completion Rate"
          value={`${completionRate}%`}
          icon={<TrendingUp className="h-5 w-5" />}
          changeType={
            parseFloat(completionRate) >= 80 ? "positive" : "negative"
          }
          change={
            parseFloat(completionRate) >= 80 ? "Healthy" : "Below target"
          }
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Deliveries This Week — Bar Chart */}
        <ChartCard
          title="Deliveries This Week"
          subtitle="Daily delivery volume"
        >
          {dailyTrendData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={dailyTrendData}
                margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
              >
                <defs>
                  <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#00a4e4" stopOpacity={1} />
                    <stop offset="100%" stopColor="#00a4e4" stopOpacity={0.2} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 6" stroke="#f0f0f0" />
                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 11, fill: "#94a3b8" }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: "#94a3b8" }}
                  axisLine={false}
                  tickLine={false}
                  allowDecimals={false}
                />
                <Tooltip content={<BarChartTooltip />} cursor={{ fill: "rgba(0, 164, 228, 0.06)" }} />
                <Bar
                  dataKey="count"
                  fill="url(#barGradient)"
                  radius={[6, 6, 0, 0]}
                  maxBarSize={48}
                  animationDuration={800}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[300px] text-sm text-text-muted">
              No data for this period
            </div>
          )}
        </ChartCard>

        {/* Revenue by Payment Method — Pie Chart */}
        <ChartCard
          title="Revenue by Payment Method"
          subtitle="Breakdown of payment types"
        >
          {paymentMethodData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={paymentMethodData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={105}
                  paddingAngle={4}
                  dataKey="value"
                  nameKey="name"
                  stroke="none"
                  strokeWidth={2}
                  animationDuration={800}
                  label={({ percent }) => `${(percent * 100).toFixed(0)}%`}
                >
                  {paymentMethodData.map((_, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={CHART_COLORS[index % CHART_COLORS.length]}
                      strokeWidth={1}
                      stroke="#fff"
                    />
                  ))}
                </Pie>
                <Tooltip content={<PieChartTooltip />} cursor={{ fill: "rgba(0, 164, 228, 0.06)" }} />
                <Legend
                  verticalAlign="bottom"
                  height={36}
                  iconType="circle"
                  formatter={(value: string) => (
                    <span className="text-sm text-text-dark">{value}</span>
                  )}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[300px] text-sm text-text-muted">
              No payment data available
            </div>
          )}
        </ChartCard>
      </div>

      {/* Recent Deliveries Table */}
      <div>
        <h2 className="text-lg font-semibold text-text-dark mb-3">
          Recent Deliveries
        </h2>
        <DataTable
          columns={recentDeliveriesColumns}
          data={recentDeliveries}
          loading={loading && recentDeliveries.length === 0}
        />
      </div>
    </div>
  );
}
