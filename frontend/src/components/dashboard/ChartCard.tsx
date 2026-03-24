"use client";

import React from "react";

interface ChartCardProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
}

export default function ChartCard({
  title,
  subtitle,
  children,
  className = "",
}: ChartCardProps) {
  return (
    <div
      className={`rounded-xl bg-white p-5 shadow-sm border border-gray-100 ${className}`}
    >
      {/* Header */}
      <div className="mb-4">
        <h3 className="text-base font-semibold text-text-dark">{title}</h3>
        {subtitle && (
          <p className="mt-0.5 text-sm text-text-muted">{subtitle}</p>
        )}
      </div>

      {/* Chart area — full width so recharts ResponsiveContainer works correctly */}
      <div className="w-full">{children}</div>
    </div>
  );
}
