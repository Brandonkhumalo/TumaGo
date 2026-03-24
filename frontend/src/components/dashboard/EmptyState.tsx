"use client";

import React from "react";
import { Inbox } from "lucide-react";

interface EmptyStateProps {
  icon?: React.ReactNode;
  title?: string;
  description?: string;
}

export default function EmptyState({
  icon,
  title = "No data found",
  description = "There are no records to display at this time.",
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-gray-200 bg-gray-50/50 px-6 py-16 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-text-muted">
        {icon ?? <Inbox className="h-7 w-7" />}
      </div>
      <h3 className="text-base font-semibold text-text-dark">{title}</h3>
      <p className="max-w-sm text-sm text-text-muted">{description}</p>
    </div>
  );
}
