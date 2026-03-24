"use client";

import React, { useEffect, useState, useCallback } from "react";
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
  DollarSign,
  TrendingUp,
  CheckCircle,
  XCircle,
  Clock,
  Wallet,
  Banknote,
} from "lucide-react";
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
} from "recharts";

// ── Types ────────────────────────────────────────────────────────────────

/** Shorthand alias so column render functions satisfy DataTable's generic */
type Row = Record<string, unknown>;

interface PaymentMethodBreakdown {
  payment_method: string;
  total: number;
  count: number;
}

interface FinancialMetrics {
  total_revenue: number;
  by_payment_method: PaymentMethodBreakdown[];
  avg_fare: number;
  payment_success_rate: number;
  failed_payments_count: number;
  pending_payments_count: number;
  total_owed_to_drivers: number;
  total_paid_to_drivers: number;
}

interface PaymentsListResponse {
  results: Row[];
  total: number;
  page: number;
  total_pages: number;
}

// ── Helpers ──────────────────────────────────────────────────────────────

const CHART_COLORS = [
  "#00a4e4",
  "#0e74bc",
  "#FF6B35",
  "#10b981",
  "#8b5cf6",
  "#f59e0b",
];

function formatCurrency(value: number): string {
  return `$${value.toFixed(2)}`;
}

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

function truncateId(id: string): string {
  if (id.length <= 12) return id;
  return `${id.slice(0, 8)}...`;
}

/**
 * Convert PeriodSelector value ("today", "week", "month", or "custom:start:end")
 * into URL search params the backend understands.
 */
function periodToParams(period: string): string {
  const params = new URLSearchParams();
  if (period === "today") {
    params.set("period", "today");
  } else if (period === "week") {
    params.set("period", "week");
  } else if (period === "month") {
    params.set("period", "month");
  } else if (period.startsWith("custom:")) {
    const [, start, end] = period.split(":");
    if (start && end) {
      params.set("start_date", start);
      params.set("end_date", end);
    }
  }
  return params.toString();
}

// ── Static column definitions ────────────────────────────────────────────

const paymentColumns = [
  {
    key: "id",
    header: "ID",
    render: (row: Row) => {
      const id = (row.id as string) ?? "";
      return (
        <span className="font-mono text-xs text-text-muted" title={id}>
          {truncateId(id)}
        </span>
      );
    },
  },
  {
    key: "client_name",
    header: "Client",
    render: (row: Row) => (
      <span className="font-medium text-text-dark">
        {(row.client_name as string) ||
          (row.client_email as string) ||
          "---"}
      </span>
    ),
  },
  {
    key: "amount",
    header: "Amount",
    sortable: true,
    render: (row: Row) => (
      <span className="font-semibold">
        {formatCurrency(row.amount as number)}
      </span>
    ),
  },
  {
    key: "payment_method",
    header: "Method",
    render: (row: Row) => (
      <StatusBadge status={row.payment_method as string} variant="info" />
    ),
  },
  {
    key: "status",
    header: "Status",
    render: (row: Row) => <StatusBadge status={row.status as string} />,
  },
  {
    key: "created_at",
    header: "Date",
    sortable: true,
    render: (row: Row) => (
      <span className="text-text-muted text-xs">
        {formatDate(row.created_at as string)}
      </span>
    ),
  },
];

// ── Page ─────────────────────────────────────────────────────────────────

export default function FinancialsPage() {
  const { token } = useAuth();

  // Period selector
  const [period, setPeriod] = useState("month");

  // Metrics state
  const [metrics, setMetrics] = useState<FinancialMetrics | null>(null);
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [metricsError, setMetricsError] = useState<string | null>(null);

  // Payments list state
  const [payments, setPayments] = useState<PaymentsListResponse | null>(null);
  const [paymentsLoading, setPaymentsLoading] = useState(true);
  const [paymentsPage, setPaymentsPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [methodFilter, setMethodFilter] = useState<string>("all");

  // ── Fetch metrics ────────────────────────────────────────────────────

  const fetchMetrics = useCallback(async () => {
    if (!token) return;
    setMetricsLoading(true);
    setMetricsError(null);
    try {
      const params = periodToParams(period);
      const data = await adminAPI.financialMetrics(
        token,
        params || undefined
      );
      setMetrics(data);
    } catch (err) {
      setMetricsError(
        err instanceof Error
          ? err.message
          : "Failed to load financial metrics"
      );
    } finally {
      setMetricsLoading(false);
    }
  }, [token, period]);

  // ── Fetch payments list ──────────────────────────────────────────────

  const fetchPayments = useCallback(async () => {
    if (!token) return;
    setPaymentsLoading(true);
    try {
      const params = new URLSearchParams();
      params.set("page", String(paymentsPage));
      if (statusFilter !== "all") params.set("status", statusFilter);
      if (methodFilter !== "all") params.set("payment_method", methodFilter);

      // Include period params too
      const periodParams = periodToParams(period);
      if (periodParams) {
        const pp = new URLSearchParams(periodParams);
        pp.forEach((v, k) => params.set(k, v));
      }

      const data = await adminAPI.paymentsList(token, params.toString());
      setPayments(data);
    } catch {
      // Silently handle
    } finally {
      setPaymentsLoading(false);
    }
  }, [token, paymentsPage, statusFilter, methodFilter, period]);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  useEffect(() => {
    fetchPayments();
  }, [fetchPayments]);

  // Reset page on filter changes
  const handleStatusFilter = useCallback((status: string) => {
    setStatusFilter(status);
    setPaymentsPage(1);
  }, []);

  const handleMethodFilter = useCallback((method: string) => {
    setMethodFilter(method);
    setPaymentsPage(1);
  }, []);

  // ── Pie chart data ───────────────────────────────────────────────────

  const pieData = (metrics?.by_payment_method ?? []).map((m) => ({
    name: m.payment_method,
    value: m.total,
    count: m.count,
  }));

  // ── Loading state ────────────────────────────────────────────────────

  if (metricsLoading && !metrics) {
    return <LoadingSpinner text="Loading financial analytics..." size="lg" />;
  }

  // ── Error state ──────────────────────────────────────────────────────

  if (metricsError && !metrics) {
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

  // ── Render ───────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Page header + period selector */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-text-dark">
            Financial Analytics
          </h1>
          <p className="mt-1 text-sm text-text-muted">
            Revenue, payments, and driver payout overview.
          </p>
        </div>
        <PeriodSelector value={period} onChange={setPeriod} />
      </div>

      {/* Stats row */}
      {metrics && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
          <StatsCard
            title="Total Revenue"
            value={formatCurrency(metrics.total_revenue)}
            icon={<DollarSign className="h-5 w-5" />}
          />
          <StatsCard
            title="Avg Fare"
            value={formatCurrency(metrics.avg_fare)}
            icon={<TrendingUp className="h-5 w-5" />}
          />
          <StatsCard
            title="Success Rate"
            value={`${metrics.payment_success_rate.toFixed(1)}%`}
            icon={<CheckCircle className="h-5 w-5" />}
            changeType={
              metrics.payment_success_rate >= 90 ? "positive" : "negative"
            }
            change={
              metrics.payment_success_rate >= 90
                ? "Healthy"
                : "Needs attention"
            }
          />
          <StatsCard
            title="Failed Payments"
            value={metrics.failed_payments_count.toLocaleString()}
            icon={<XCircle className="h-5 w-5" />}
            changeType={
              metrics.failed_payments_count > 0 ? "negative" : "positive"
            }
            change={
              metrics.failed_payments_count > 0 ? "Action needed" : "None"
            }
          />
          <StatsCard
            title="Pending Payments"
            value={metrics.pending_payments_count.toLocaleString()}
            icon={<Clock className="h-5 w-5" />}
          />
        </div>
      )}

      {/* Revenue by Payment Method (Pie) + Driver Payouts side by side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pie chart */}
        <ChartCard
          title="Revenue by Payment Method"
          subtitle="Breakdown of payments across all methods"
        >
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={105}
                  paddingAngle={4}
                  dataKey="value"
                  nameKey="name"
                  animationDuration={800}
                  label={({ percent }) =>
                    `${(percent * 100).toFixed(0)}%`
                  }
                >
                  {pieData.map((_, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={CHART_COLORS[index % CHART_COLORS.length]}
                      strokeWidth={1}
                      stroke="#fff"
                    />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value: number) => formatCurrency(value)}
                  contentStyle={{
                    borderRadius: "8px",
                    border: "1px solid #e5e7eb",
                    boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1)",
                  }}
                  cursor={{ fill: "rgba(0, 164, 228, 0.06)" }}
                />
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
              No payment data for this period
            </div>
          )}
        </ChartCard>

        {/* Driver Payouts */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-text-dark">
            Driver Payouts
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <StatsCard
              title="Total Owed to Drivers"
              value={
                metrics ? formatCurrency(metrics.total_owed_to_drivers) : "$0.00"
              }
              icon={<Wallet className="h-5 w-5" />}
              changeType={
                metrics && metrics.total_owed_to_drivers > 0
                  ? "negative"
                  : "positive"
              }
              change={
                metrics && metrics.total_owed_to_drivers > 0
                  ? "Outstanding"
                  : "All clear"
              }
            />
            <StatsCard
              title="Total Paid to Drivers"
              value={
                metrics
                  ? formatCurrency(metrics.total_paid_to_drivers)
                  : "$0.00"
              }
              icon={<Banknote className="h-5 w-5" />}
              changeType="positive"
              change="Completed"
            />
          </div>
        </div>
      </div>

      {/* All Payments table */}
      <div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-3">
          <h2 className="text-lg font-semibold text-text-dark">
            All Payments
          </h2>

          {/* Filters */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Status filter */}
            <div className="flex items-center gap-1 rounded-lg bg-gray-100 p-1">
              {["all", "paid", "pending", "failed"].map((status) => (
                <button
                  key={status}
                  onClick={() => handleStatusFilter(status)}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors capitalize ${
                    statusFilter === status
                      ? "bg-white text-text-dark shadow-sm"
                      : "text-text-muted hover:text-text-dark"
                  }`}
                >
                  {status === "all" ? "All Status" : status}
                </button>
              ))}
            </div>

            {/* Method filter */}
            <div className="flex items-center gap-1 rounded-lg bg-gray-100 p-1">
              {["all", "EcoCash", "OneMoney", "Card"].map((method) => (
                <button
                  key={method}
                  onClick={() => handleMethodFilter(method)}
                  className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                    methodFilter === method
                      ? "bg-white text-text-dark shadow-sm"
                      : "text-text-muted hover:text-text-dark"
                  }`}
                >
                  {method === "all" ? "All Methods" : method}
                </button>
              ))}
            </div>
          </div>
        </div>

        <DataTable
          columns={paymentColumns}
          data={payments?.results ?? []}
          loading={paymentsLoading}
          pagination={
            payments
              ? {
                  page: payments.page,
                  totalPages: payments.total_pages,
                  total: payments.total,
                  onPageChange: setPaymentsPage,
                }
              : undefined
          }
        />
      </div>
    </div>
  );
}
