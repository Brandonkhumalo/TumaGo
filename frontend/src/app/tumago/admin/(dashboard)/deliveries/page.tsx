"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";
import {
  StatsCard,
  DataTable,
  ChartCard,
  PeriodSelector,
  StatusBadge,
  LoadingSpinner,
} from "@/components/dashboard";
import {
  Package,
  CheckCircle2,
  XCircle,
  TrendingUp,
  DollarSign,
  AlertCircle,
  RefreshCw,
  Filter,
} from "lucide-react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  Area,
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

interface DeliveriesListResponse {
  results: DeliveryRecord[];
  total: number;
  page: number;
  page_size: number;
  total_pages: number;
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

const STATUS_OPTIONS = [
  { value: "", label: "All Statuses" },
  { value: "successful", label: "Successful" },
  { value: "cancelled", label: "Cancelled" },
];

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

function formatHour(hour: number): string {
  if (hour === 0) return "12 AM";
  if (hour === 12) return "12 PM";
  return hour < 12 ? `${hour} AM` : `${hour - 12} PM`;
}

function truncateId(id: string): string {
  if (!id) return "--";
  return id.length > 8 ? `${id.slice(0, 8)}...` : id;
}

/**
 * Builds a query-string from the period selector value.
 * "today" -> "period=today", "week" -> "period=week",
 * "custom:2024-01-01:2024-01-31" -> "start_date=2024-01-01&end_date=2024-01-31"
 */
function periodToParams(period: string): string {
  if (period.startsWith("custom:")) {
    const [, start, end] = period.split(":");
    return `start_date=${start}&end_date=${end}`;
  }
  return `period=${period}`;
}

// ── Custom Tooltips ──────────────────────────────────────────────────────

function HourTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: { value: number }[];
  label?: number;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg bg-white px-3 py-2 shadow-lg border border-gray-100 text-sm">
      <p className="font-medium text-text-dark">
        {formatHour(label ?? 0)}
      </p>
      <p className="text-text-muted">
        {payload[0].value.toLocaleString()} deliveries
      </p>
    </div>
  );
}

function VehicleTooltip({
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
        {payload[0].value.toLocaleString()} deliveries
      </p>
    </div>
  );
}

function TrendTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: { dataKey: string; value: number; color: string }[];
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg bg-white px-3 py-2 shadow-lg border border-gray-100 text-sm">
      <p className="font-medium text-text-dark mb-1">{label}</p>
      {payload.map((entry) => (
        <p key={entry.dataKey} className="text-text-muted flex items-center gap-2">
          <span
            className="inline-block h-2 w-2 rounded-full"
            style={{ backgroundColor: entry.color }}
          />
          {entry.dataKey === "count"
            ? `${entry.value.toLocaleString()} deliveries`
            : formatCurrency(entry.value)}
        </p>
      ))}
    </div>
  );
}

// ── Table Columns ────────────────────────────────────────────────────────

const deliveryColumns = [
  {
    key: "id",
    header: "ID",
    render: (item: DeliveryRecord) => (
      <span className="font-mono text-xs text-text-muted" title={item.id}>
        {truncateId(item.id)}
      </span>
    ),
  },
  {
    key: "driver_name",
    header: "Driver",
    sortable: true,
  },
  {
    key: "client_name",
    header: "Client",
    sortable: true,
  },
  {
    key: "fare",
    header: "Fare",
    sortable: true,
    render: (item: DeliveryRecord) => (
      <span className="font-medium">{formatCurrency(item.fare ?? 0)}</span>
    ),
  },
  {
    key: "payment_method",
    header: "Payment Method",
    render: (item: DeliveryRecord) => (
      <span className="capitalize">
        {item.payment_method?.replace(/_/g, " ") ?? "--"}
      </span>
    ),
  },
  {
    key: "status",
    header: "Status",
    render: (item: DeliveryRecord) => (
      <StatusBadge status={item.status ?? "unknown"} />
    ),
  },
  {
    key: "created_at",
    header: "Date",
    sortable: true,
    render: (item: DeliveryRecord) =>
      item.created_at ? formatDate(item.created_at) : "--",
  },
];

// ── Deliveries Page ──────────────────────────────────────────────────────

export default function DeliveriesPage() {
  const { token } = useAuth();

  // Period / filters
  const [period, setPeriod] = useState("week");
  const [statusFilter, setStatusFilter] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);

  // Data
  const [metrics, setMetrics] = useState<DeliveryMetrics | null>(null);
  const [deliveries, setDeliveries] = useState<DeliveriesListResponse | null>(
    null
  );
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Debounce timer for search
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── Fetch metrics ────────────────────────────────────────────────────

  const fetchMetrics = useCallback(async () => {
    if (!token) return;
    setMetricsLoading(true);
    setError(null);

    try {
      const params = periodToParams(period);
      const res = await adminAPI.deliveryMetrics(token, params);
      setMetrics(res);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load delivery metrics"
      );
    } finally {
      setMetricsLoading(false);
    }
  }, [token, period]);

  // ── Fetch deliveries list ────────────────────────────────────────────

  const fetchDeliveries = useCallback(async () => {
    if (!token) return;
    setTableLoading(true);

    try {
      const params = new URLSearchParams();
      params.set("page", String(page));
      params.set("page_size", "15");
      if (statusFilter) params.set("status", statusFilter);
      if (searchQuery) params.set("search", searchQuery);

      // Include period params
      const periodParams = periodToParams(period);
      periodParams.split("&").forEach((p) => {
        const [k, v] = p.split("=");
        if (k && v) params.set(k, v);
      });

      const res = await adminAPI.deliveriesList(token, params.toString());
      // Map backend field names to frontend expectations
      if (res.results) {
        res.results = res.results.map((d: Record<string, unknown>) => ({
          ...d,
          id: d.delivery_id || d.id || "",
          status: d.successful === true ? "successful" : d.successful === false ? "cancelled" : "unknown",
          created_at: d.date || d.created_at || "",
        }));
      }
      setDeliveries(res);
    } catch (err) {
      if (!error) {
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load deliveries list"
        );
      }
    } finally {
      setTableLoading(false);
    }
  }, [token, page, statusFilter, searchQuery, period, error]);

  // Fetch metrics when period changes
  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  // Fetch table data when filters change
  useEffect(() => {
    fetchDeliveries();
  }, [fetchDeliveries]);

  // Reset page when filters change
  useEffect(() => {
    setPage(1);
  }, [period, statusFilter, searchQuery]);

  // Debounced search
  const handleSearch = useCallback((query: string) => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => {
      setSearchQuery(query);
    }, 400);
  }, []);

  // ── Loading state ────────────────────────────────────────────────────

  if (metricsLoading && !metrics) {
    return <LoadingSpinner text="Loading delivery analytics..." size="lg" />;
  }

  // ── Error state (full-page, no data) ─────────────────────────────────

  if (error && !metrics) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-16">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-50 text-red-500">
          <AlertCircle className="h-7 w-7" />
        </div>
        <h3 className="text-base font-semibold text-text-dark">
          Failed to load delivery analytics
        </h3>
        <p className="text-sm text-text-muted max-w-md text-center">{error}</p>
        <button
          onClick={() => {
            setError(null);
            fetchMetrics();
            fetchDeliveries();
          }}
          className="mt-2 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white transition hover:bg-primary-dark"
        >
          <RefreshCw className="h-4 w-4" />
          Retry
        </button>
      </div>
    );
  }

  // Prepare chart data
  const hourlyData = (metrics?.by_hour ?? []).map((d) => ({
    ...d,
    label: formatHour(d.hour),
  }));

  const vehicleData = metrics?.by_vehicle ?? [];

  const dailyTrendData = (metrics?.daily_trend ?? []).map((d) => ({
    ...d,
    label: formatShortDate(d.date),
  }));

  // ── Render ───────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-text-dark">
          Delivery Analytics
        </h1>
        <PeriodSelector value={period} onChange={setPeriod} />
      </div>

      {/* Error banner (non-blocking) */}
      {error && metrics && (
        <div className="flex items-center gap-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Stats row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <StatsCard
          title="Total Deliveries"
          value={(metrics?.total ?? 0).toLocaleString()}
          icon={<Package className="h-5 w-5" />}
        />
        <StatsCard
          title="Successful"
          value={(metrics?.successful ?? 0).toLocaleString()}
          icon={<CheckCircle2 className="h-5 w-5" />}
          changeType="positive"
          change={
            metrics && metrics.total > 0
              ? `${((metrics.successful / metrics.total) * 100).toFixed(0)}%`
              : undefined
          }
        />
        <StatsCard
          title="Cancelled"
          value={(metrics?.cancelled ?? 0).toLocaleString()}
          icon={<XCircle className="h-5 w-5" />}
          changeType={
            (metrics?.cancelled ?? 0) > 0 ? "negative" : "neutral"
          }
          change={
            metrics && metrics.total > 0
              ? `${((metrics.cancelled / metrics.total) * 100).toFixed(0)}%`
              : undefined
          }
        />
        <StatsCard
          title="Completion Rate"
          value={`${(metrics?.completion_rate ?? 0).toFixed(1)}%`}
          icon={<TrendingUp className="h-5 w-5" />}
          changeType={
            (metrics?.completion_rate ?? 0) >= 80 ? "positive" : "negative"
          }
          change={
            (metrics?.completion_rate ?? 0) >= 80 ? "Healthy" : "Below target"
          }
        />
        <StatsCard
          title="Avg Fare"
          value={formatCurrency(metrics?.avg_fare ?? 0)}
          icon={<DollarSign className="h-5 w-5" />}
        />
      </div>

      {/* Charts: Hourly + Vehicle Type */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Deliveries by Hour */}
        <ChartCard
          title="Deliveries by Hour"
          subtitle="Demand distribution across the day"
        >
          {hourlyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={hourlyData}
                margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
              >
                <defs>
                  <linearGradient id="hourBarGradient" x1="0" y1="0" x2="0" y2="1">
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
                  interval={2}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: "#94a3b8" }}
                  axisLine={false}
                  tickLine={false}
                  allowDecimals={false}
                />
                <Tooltip content={<HourTooltip />} cursor={{ fill: "rgba(0, 164, 228, 0.06)" }} />
                <Bar
                  dataKey="count"
                  fill="url(#hourBarGradient)"
                  radius={[6, 6, 0, 0]}
                  maxBarSize={32}
                  animationDuration={800}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[300px] text-sm text-text-muted">
              No hourly data available
            </div>
          )}
        </ChartCard>

        {/* By Vehicle Type */}
        <ChartCard
          title="By Vehicle Type"
          subtitle="Delivery distribution by vehicle"
        >
          {vehicleData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={vehicleData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={105}
                  paddingAngle={4}
                  dataKey="value"
                  nameKey="name"
                  stroke="none"
                  animationDuration={800}
                  label={({ percent }) => `${(percent * 100).toFixed(0)}%`}
                >
                  {vehicleData.map((_, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={CHART_COLORS[index % CHART_COLORS.length]}
                      strokeWidth={1}
                      stroke="#fff"
                    />
                  ))}
                </Pie>
                <Tooltip content={<VehicleTooltip />} cursor={{ fill: "rgba(0, 164, 228, 0.06)" }} />
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
              No vehicle data available
            </div>
          )}
        </ChartCard>
      </div>

      {/* Daily Trend — full width */}
      <ChartCard
        title="Daily Trend"
        subtitle="Delivery count and revenue over time"
      >
        {dailyTrendData.length > 0 ? (
          <ResponsiveContainer width="100%" height={320}>
            <LineChart
              data={dailyTrendData}
              margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
            >
              <defs>
                <linearGradient id="trendCountGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#00a4e4" stopOpacity={0.2} />
                  <stop offset="100%" stopColor="#00a4e4" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="trendRevenueGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity={0.2} />
                  <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
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
                yAxisId="left"
                tick={{ fontSize: 11, fill: "#94a3b8" }}
                axisLine={false}
                tickLine={false}
                allowDecimals={false}
              />
              <YAxis
                yAxisId="right"
                orientation="right"
                tick={{ fontSize: 11, fill: "#94a3b8" }}
                axisLine={false}
                tickLine={false}
                tickFormatter={(v: number) => `$${v}`}
              />
              <Tooltip content={<TrendTooltip />} cursor={{ stroke: "#e2e8f0" }} />
              <Legend
                verticalAlign="top"
                height={36}
                iconType="circle"
                formatter={(value: string) => (
                  <span className="text-sm text-text-dark capitalize">
                    {value === "count" ? "Deliveries" : "Revenue"}
                  </span>
                )}
              />
              <Area
                yAxisId="left"
                type="natural"
                dataKey="count"
                fill="url(#trendCountGradient)"
                stroke="none"
              />
              <Area
                yAxisId="right"
                type="natural"
                dataKey="revenue"
                fill="url(#trendRevenueGradient)"
                stroke="none"
              />
              <Line
                yAxisId="left"
                type="natural"
                dataKey="count"
                stroke={CHART_COLORS[0]}
                strokeWidth={2}
                dot={{ r: 3, fill: CHART_COLORS[0] }}
                activeDot={{ r: 6, strokeWidth: 2, stroke: "#fff" }}
              />
              <Line
                yAxisId="right"
                type="natural"
                dataKey="revenue"
                stroke={CHART_COLORS[3]}
                strokeWidth={2}
                dot={{ r: 3, fill: CHART_COLORS[3] }}
                activeDot={{ r: 6, strokeWidth: 2, stroke: "#fff" }}
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex items-center justify-center h-[320px] text-sm text-text-muted">
            No trend data available
          </div>
        )}
      </ChartCard>

      {/* All Deliveries Table */}
      <div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-3">
          <h2 className="text-lg font-semibold text-text-dark">
            All Deliveries
          </h2>

          {/* Status filter */}
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-text-muted" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-text-dark outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <DataTable
          columns={deliveryColumns}
          data={(deliveries?.results ?? []) as DeliveryRecord[]}
          loading={tableLoading}
          onSearch={handleSearch}
          searchPlaceholder="Search by driver, client, or ID..."
          pagination={
            deliveries && deliveries.total_pages > 1
              ? {
                  page: deliveries.page,
                  totalPages: deliveries.total_pages,
                  total: deliveries.total,
                  onPageChange: setPage,
                }
              : undefined
          }
        />
      </div>
    </div>
  );
}
