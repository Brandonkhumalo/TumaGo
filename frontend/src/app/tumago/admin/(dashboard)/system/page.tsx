"use client";

import React, { useEffect, useState, useCallback } from "react";
import {
  Activity,
  Server,
  Database,
  Wifi,
  Globe,
  Zap,
  Bell,
  BarChart3,
  ExternalLink,
  RefreshCw,
  Info,
} from "lucide-react";
import { LoadingSpinner } from "@/components/dashboard";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";

// ---------- Types ----------

interface ServiceInfo {
  name: string;
  status: string;
  note?: string;
}

interface SystemInfo {
  django_version?: string;
  python_version?: string;
  timestamp?: string;
}

interface SystemHealthData {
  db_status: string;
  redis_status: string;
  websocket_status: string;
  services: ServiceInfo[];
  system_info: SystemInfo;
}

// ---------- Constants ----------

/** Mapping of well-known service names to icons and descriptions. */
const SERVICE_CONFIG: Record<
  string,
  { icon: React.ReactNode; description: string }
> = {
  gateway: {
    icon: <Globe className="h-5 w-5" />,
    description: "API routing, rate limiting, JWT verification",
  },
  location: {
    icon: <Wifi className="h-5 w-5" />,
    description: "WebSocket GPS streaming, Redis GEO",
  },
  matching: {
    icon: <Zap className="h-5 w-5" />,
    description: "GEOSEARCH driver matching",
  },
  django: {
    icon: <Server className="h-5 w-5" />,
    description: "Auth, CRUD, admin, delivery management",
  },
  notification: {
    icon: <Bell className="h-5 w-5" />,
    description: "FCM push notifications via Redis pub/sub",
  },
  postgresql: {
    icon: <Database className="h-5 w-5" />,
    description: "Primary database with PgBouncer pooling",
  },
  redis: {
    icon: <Database className="h-5 w-5" />,
    description: "Cache, GEO, Dramatiq broker, pub/sub",
  },
};

/** Friendly display names for services. */
const SERVICE_DISPLAY_NAMES: Record<string, string> = {
  gateway: "API Gateway (Go)",
  location: "Location Service (Go)",
  matching: "Matching Service (Go)",
  django: "Django Backend",
  notification: "Notification Service (FastAPI)",
  postgresql: "PostgreSQL + PgBouncer",
  redis: "Redis",
};

/** Monitoring links — informational placeholders. */
const MONITORING_LINKS = [
  {
    title: "Prometheus Dashboard",
    description: "Service metrics, request rates, error rates",
    icon: <BarChart3 className="h-5 w-5" />,
    url: "/prometheus",
  },
  {
    title: "PgBouncer Stats",
    description: "Connection pool utilization and query stats",
    icon: <Database className="h-5 w-5" />,
    url: "/pgbouncer/stats",
  },
  {
    title: "Redis Info",
    description: "Memory usage, connected clients, keyspace",
    icon: <Database className="h-5 w-5" />,
    url: "/redis/info",
  },
];

// ---------- Helpers ----------

/** Returns the appropriate Tailwind color class for a status string. */
function getStatusColor(status: string): string {
  const s = status.toLowerCase();
  if (s === "healthy" || s === "connected" || s === "ok" || s === "up") {
    return "#10b981"; // green
  }
  if (s === "degraded" || s === "warning" || s === "slow") {
    return "#f59e0b"; // yellow
  }
  return "#ef4444"; // red
}

/** Returns a human-readable label for a status string. */
function getStatusLabel(status: string): string {
  const s = status.toLowerCase();
  if (s === "healthy" || s === "connected" || s === "ok" || s === "up") {
    return "Healthy";
  }
  if (s === "degraded" || s === "warning" || s === "slow") {
    return "Degraded";
  }
  if (s === "unknown" || s === "") return "Unknown";
  return "Down";
}

// ---------- Sub-components ----------

function StatusDot({ status }: { status: string }) {
  const color = getStatusColor(status);
  return (
    <span className="relative flex h-3 w-3">
      {/* Pulse ring for healthy status */}
      {(status.toLowerCase() === "healthy" ||
        status.toLowerCase() === "connected" ||
        status.toLowerCase() === "ok" ||
        status.toLowerCase() === "up") && (
        <span
          className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-30"
          style={{ backgroundColor: color }}
        />
      )}
      <span
        className="relative inline-flex h-3 w-3 rounded-full"
        style={{ backgroundColor: color }}
      />
    </span>
  );
}

interface ServiceCardProps {
  name: string;
  status: string;
  note?: string;
  icon: React.ReactNode;
  description: string;
}

function ServiceCard({ name, status, note, icon, description }: ServiceCardProps) {
  const statusLabel = getStatusLabel(status);
  const statusColor = getStatusColor(status);

  return (
    <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100 transition-shadow duration-200 hover:shadow-md">
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-gray-50 text-text-muted">
            {icon}
          </div>
          <div className="min-w-0">
            <h3 className="text-sm font-semibold text-text-dark">{name}</h3>
            <p className="mt-0.5 text-xs text-text-muted">{description}</p>
          </div>
        </div>
        <div className="flex items-center gap-2 shrink-0 ml-2">
          <StatusDot status={status} />
          <span
            className="text-xs font-semibold"
            style={{ color: statusColor }}
          >
            {statusLabel}
          </span>
        </div>
      </div>
      {note && (
        <p className="mt-3 rounded-lg bg-gray-50 px-3 py-2 text-xs text-text-muted">
          {note}
        </p>
      )}
    </div>
  );
}

function MonitoringLinkCard({
  title,
  description,
  icon,
  url,
}: {
  title: string;
  description: string;
  icon: React.ReactNode;
  url: string;
}) {
  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className="group flex items-start gap-3 rounded-xl bg-white p-5 shadow-sm border border-gray-100 transition-all duration-200 hover:shadow-md hover:border-primary/30"
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-light text-primary">
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <h3 className="text-sm font-semibold text-text-dark group-hover:text-primary transition-colors">
            {title}
          </h3>
          <ExternalLink className="h-3.5 w-3.5 text-text-muted opacity-0 group-hover:opacity-100 transition-opacity" />
        </div>
        <p className="mt-0.5 text-xs text-text-muted">{description}</p>
      </div>
    </a>
  );
}

// ---------- Build service list ----------

/**
 * Merges the API response with our known service configuration.
 * If the API returns services, we use those. Otherwise, we build
 * a fallback list from the individual status fields.
 */
function buildServiceList(data: SystemHealthData): ServiceCardProps[] {
  // If the API returns a populated services array, prefer that.
  if (data.services && data.services.length > 0) {
    return data.services.map((svc) => {
      const key = svc.name.toLowerCase().replace(/[\s_-]+/g, "");
      // Try to match against our known config by checking if the key contains a known service name
      let config = SERVICE_CONFIG["django"]; // default fallback
      let displayName = svc.name;
      for (const [configKey, configVal] of Object.entries(SERVICE_CONFIG)) {
        if (key.includes(configKey)) {
          config = configVal;
          displayName = SERVICE_DISPLAY_NAMES[configKey] || svc.name;
          break;
        }
      }
      return {
        name: displayName,
        status: svc.status,
        note: svc.note,
        icon: config.icon,
        description: config.description,
      };
    });
  }

  // Fallback: build from individual status fields
  const fallback: ServiceCardProps[] = [
    {
      name: SERVICE_DISPLAY_NAMES.gateway,
      status: "unknown",
      icon: SERVICE_CONFIG.gateway.icon,
      description: SERVICE_CONFIG.gateway.description,
      note: "Status not reported by API",
    },
    {
      name: SERVICE_DISPLAY_NAMES.location,
      status: data.websocket_status || "unknown",
      icon: SERVICE_CONFIG.location.icon,
      description: SERVICE_CONFIG.location.description,
      note: data.websocket_status ? `WebSocket: ${data.websocket_status}` : undefined,
    },
    {
      name: SERVICE_DISPLAY_NAMES.matching,
      status: "unknown",
      icon: SERVICE_CONFIG.matching.icon,
      description: SERVICE_CONFIG.matching.description,
      note: "Status not reported by API",
    },
    {
      name: SERVICE_DISPLAY_NAMES.django,
      status: "healthy",
      icon: SERVICE_CONFIG.django.icon,
      description: SERVICE_CONFIG.django.description,
      note: "Responding to health check",
    },
    {
      name: SERVICE_DISPLAY_NAMES.notification,
      status: "unknown",
      icon: SERVICE_CONFIG.notification.icon,
      description: SERVICE_CONFIG.notification.description,
      note: "Status not reported by API",
    },
    {
      name: SERVICE_DISPLAY_NAMES.postgresql,
      status: data.db_status || "unknown",
      icon: SERVICE_CONFIG.postgresql.icon,
      description: SERVICE_CONFIG.postgresql.description,
    },
    {
      name: SERVICE_DISPLAY_NAMES.redis,
      status: data.redis_status || "unknown",
      icon: SERVICE_CONFIG.redis.icon,
      description: SERVICE_CONFIG.redis.description,
    },
  ];

  return fallback;
}

// ---------- Page Component ----------

export default function SystemHealthPage() {
  const { token } = useAuth();
  const [data, setData] = useState<SystemHealthData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);

  const fetchData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const result = await adminAPI.systemHealth(token);
      setData(result);
      setLastRefresh(new Date());
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load system health"
      );
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // ---------- Loading ----------

  if (loading) {
    return <LoadingSpinner text="Checking system health..." size="lg" />;
  }

  // ---------- Error ----------

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold text-text-dark">System Health</h1>
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

  // ---------- Build derived data ----------

  const services = data ? buildServiceList(data) : [];
  const healthyCount = services.filter(
    (s) =>
      s.status.toLowerCase() === "healthy" ||
      s.status.toLowerCase() === "connected" ||
      s.status.toLowerCase() === "ok" ||
      s.status.toLowerCase() === "up"
  ).length;

  // ---------- Render ----------

  return (
    <div className="space-y-8">
      {/* Page header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-text-dark">System Health</h1>
          <p className="mt-1 text-sm text-text-muted">
            Monitor service status and infrastructure health
          </p>
        </div>
        <div className="flex items-center gap-3">
          {lastRefresh && (
            <span className="text-xs text-text-muted">
              Last checked:{" "}
              {lastRefresh.toLocaleTimeString("en-US", {
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
              })}
            </span>
          )}
          <button
            onClick={fetchData}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-medium text-text-dark shadow-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Overview banner */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-light text-primary">
            <Activity className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-medium text-text-muted">
              Overall Status
            </p>
            <p className="text-lg font-bold text-text-dark">
              {healthyCount} / {services.length} services healthy
            </p>
          </div>
        </div>
      </div>

      {/* Service Status Grid */}
      <div>
        <h2 className="mb-4 text-lg font-semibold text-text-dark">
          Service Status
        </h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((svc) => (
            <ServiceCard
              key={svc.name}
              name={svc.name}
              status={svc.status}
              note={svc.note}
              icon={svc.icon}
              description={svc.description}
            />
          ))}
        </div>
      </div>

      {/* Monitoring Links */}
      <div>
        <h2 className="mb-4 text-lg font-semibold text-text-dark">
          Monitoring Links
        </h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {MONITORING_LINKS.map((link) => (
            <MonitoringLinkCard
              key={link.title}
              title={link.title}
              description={link.description}
              icon={link.icon}
              url={link.url}
            />
          ))}
        </div>
      </div>

      {/* System Info */}
      <div>
        <h2 className="mb-4 text-lg font-semibold text-text-dark">
          System Info
        </h2>
        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
          {data?.system_info ? (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                  Django Version
                </p>
                <p className="mt-1 text-sm font-semibold text-text-dark">
                  {data.system_info.django_version || "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                  Python Version
                </p>
                <p className="mt-1 text-sm font-semibold text-text-dark">
                  {data.system_info.python_version || "—"}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-text-muted">
                  Last Reported
                </p>
                <p className="mt-1 text-sm font-semibold text-text-dark">
                  {data.system_info.timestamp
                    ? new Date(data.system_info.timestamp).toLocaleString(
                        "en-US",
                        {
                          year: "numeric",
                          month: "short",
                          day: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        }
                      )
                    : "—"}
                </p>
              </div>
            </div>
          ) : (
            <p className="text-sm text-text-muted">
              System info is not available at this time.
            </p>
          )}
        </div>
      </div>

      {/* Footer note */}
      <div className="flex items-start gap-2 rounded-xl border border-blue-100 bg-blue-50/50 p-4">
        <Info className="mt-0.5 h-4 w-4 shrink-0 text-blue-500" />
        <p className="text-sm text-blue-700">
          For detailed metrics, check Prometheus dashboards and service logs.
          Real-time service metrics are collected via django-prometheus and
          prometheus-fastapi-instrumentator.
        </p>
      </div>
    </div>
  );
}
