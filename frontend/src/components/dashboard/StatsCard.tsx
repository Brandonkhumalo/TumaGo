"use client";

import React from "react";

interface StatsCardProps {
  title: string;
  value: string | number;
  change?: string;
  changeType?: "positive" | "negative" | "neutral";
  icon?: React.ReactNode;
  subtitle?: string;
}

export default function StatsCard({
  title,
  value,
  change,
  changeType = "neutral",
  icon,
  subtitle,
}: StatsCardProps) {
  const changeBadgeClasses: Record<string, string> = {
    positive: "bg-emerald-50 text-emerald-700",
    negative: "bg-red-50 text-red-700",
    neutral: "bg-gray-100 text-text-muted",
  };

  return (
    <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100 transition-shadow duration-200 hover:shadow-md">
      <div className="flex items-start gap-4">
        {/* Icon */}
        {icon && (
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary-light text-primary">
            {icon}
          </div>
        )}

        <div className="min-w-0 flex-1">
          {/* Title */}
          <p className="text-sm font-medium text-text-muted truncate">
            {title}
          </p>

          {/* Value and change badge */}
          <div className="mt-1 flex items-baseline gap-2 flex-wrap">
            <span className="text-2xl font-bold text-text-dark">{value}</span>
            {change && (
              <span
                className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${changeBadgeClasses[changeType]}`}
              >
                {change}
              </span>
            )}
          </div>

          {/* Subtitle */}
          {subtitle && (
            <p className="mt-1 text-xs text-text-muted">{subtitle}</p>
          )}
        </div>
      </div>
    </div>
  );
}
