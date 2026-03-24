"use client";

import React from "react";

interface StatusBadgeProps {
  status: string;
  variant?: "success" | "warning" | "danger" | "info" | "neutral";
}

/**
 * Maps common status strings to a visual variant so the caller does not
 * have to pass `variant` every time for well-known statuses.
 */
const STATUS_VARIANT_MAP: Record<string, StatusBadgeProps["variant"]> = {
  paid: "success",
  successful: "success",
  delivered: "success",
  active: "success",
  completed: "success",
  online: "success",
  accepted: "success",
  pending: "warning",
  matching: "warning",
  processing: "warning",
  in_progress: "warning",
  in_transit: "warning",
  failed: "danger",
  cancelled: "danger",
  rejected: "danger",
  offline: "danger",
  expired: "danger",
  info: "info",
  new: "info",
};

const VARIANT_CLASSES: Record<string, string> = {
  success: "bg-emerald-50 text-emerald-700",
  warning: "bg-amber-50 text-amber-700",
  danger: "bg-red-50 text-red-700",
  info: "bg-blue-50 text-blue-700",
  neutral: "bg-gray-100 text-text-muted",
};

export default function StatusBadge({ status, variant }: StatusBadgeProps) {
  // Determine variant: explicit prop > auto-mapped > fallback to neutral
  const resolved =
    variant ??
    STATUS_VARIANT_MAP[status.toLowerCase().replace(/\s+/g, "_")] ??
    "neutral";

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold capitalize ${VARIANT_CLASSES[resolved]}`}
    >
      {status}
    </span>
  );
}
