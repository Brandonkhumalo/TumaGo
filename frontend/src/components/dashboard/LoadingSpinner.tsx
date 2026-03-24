"use client";

import React from "react";
import { Loader2 } from "lucide-react";

interface LoadingSpinnerProps {
  text?: string;
  size?: "sm" | "md" | "lg";
}

const sizeClasses: Record<string, { icon: string; text: string }> = {
  sm: { icon: "h-4 w-4", text: "text-xs" },
  md: { icon: "h-6 w-6", text: "text-sm" },
  lg: { icon: "h-10 w-10", text: "text-base" },
};

export default function LoadingSpinner({
  text,
  size = "md",
}: LoadingSpinnerProps) {
  const classes = sizeClasses[size];

  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12">
      <Loader2 className={`${classes.icon} animate-spin text-primary`} />
      {text && (
        <p className={`${classes.text} font-medium text-text-muted`}>{text}</p>
      )}
    </div>
  );
}
