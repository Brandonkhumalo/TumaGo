"use client";

import React, { useState, useEffect } from "react";

interface PeriodSelectorProps {
  value: string;
  onChange: (period: string) => void;
}

const presetPeriods = [
  { key: "today", label: "Today" },
  { key: "week", label: "This Week" },
  { key: "month", label: "This Month" },
  { key: "custom", label: "Custom" },
];

export default function PeriodSelector({
  value,
  onChange,
}: PeriodSelectorProps) {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // When switching away from custom, clear date inputs
  useEffect(() => {
    if (value !== "custom") {
      setStartDate("");
      setEndDate("");
    }
  }, [value]);

  /**
   * When both custom dates are filled, notify the parent with a combined
   * string so it can parse the range: "custom:2024-01-01:2024-01-31"
   */
  const handleDateChange = (start: string, end: string) => {
    setStartDate(start);
    setEndDate(end);
    if (start && end) {
      onChange(`custom:${start}:${end}`);
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Preset buttons */}
      {presetPeriods.map((period) => (
        <button
          key={period.key}
          onClick={() => onChange(period.key)}
          className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
            value === period.key || (value.startsWith("custom") && period.key === "custom")
              ? "bg-primary text-white shadow-sm"
              : "bg-gray-100 text-text-dark hover:bg-gray-200"
          }`}
        >
          {period.label}
        </button>
      ))}

      {/* Custom date range inputs — only visible when "Custom" is active */}
      {(value === "custom" || value.startsWith("custom:")) && (
        <div className="flex items-center gap-2 ml-1">
          <input
            type="date"
            value={startDate}
            onChange={(e) => handleDateChange(e.target.value, endDate)}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-text-dark outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          <span className="text-sm text-text-muted">to</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => handleDateChange(startDate, e.target.value)}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-text-dark outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
        </div>
      )}
    </div>
  );
}
