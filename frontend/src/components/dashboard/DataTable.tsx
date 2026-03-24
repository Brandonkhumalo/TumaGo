"use client";

import React, { useState, useCallback } from "react";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";

interface Column<T> {
  key: string;
  header: string;
  render?: (item: T) => React.ReactNode;
  sortable?: boolean;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  pagination?: {
    page: number;
    totalPages: number;
    total: number;
    onPageChange: (page: number) => void;
  };
  onSearch?: (query: string) => void;
  searchPlaceholder?: string;
}

/**
 * Safely retrieves a nested value from an object using a dot-separated key.
 * e.g. getNestedValue({ user: { name: "John" } }, "user.name") => "John"
 */
function getNestedValue(obj: Record<string, unknown>, key: string): unknown {
  return key.split(".").reduce<unknown>((acc, part) => {
    if (acc && typeof acc === "object" && part in (acc as Record<string, unknown>)) {
      return (acc as Record<string, unknown>)[part];
    }
    return undefined;
  }, obj);
}

export default function DataTable<T extends Record<string, unknown>>({
  columns,
  data,
  loading = false,
  pagination,
  onSearch,
  searchPlaceholder = "Search...",
}: DataTableProps<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");

  const handleSort = useCallback(
    (key: string) => {
      if (sortKey === key) {
        setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
      } else {
        setSortKey(key);
        setSortDirection("asc");
      }
    },
    [sortKey]
  );

  // Sort data locally when a sortable column header is clicked
  const sortedData = React.useMemo(() => {
    if (!sortKey) return data;

    return [...data].sort((a, b) => {
      const aVal = getNestedValue(a, sortKey);
      const bVal = getNestedValue(b, sortKey);

      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;

      if (typeof aVal === "number" && typeof bVal === "number") {
        return sortDirection === "asc" ? aVal - bVal : bVal - aVal;
      }

      const aStr = String(aVal).toLowerCase();
      const bStr = String(bVal).toLowerCase();
      if (aStr < bStr) return sortDirection === "asc" ? -1 : 1;
      if (aStr > bStr) return sortDirection === "asc" ? 1 : -1;
      return 0;
    });
  }, [data, sortKey, sortDirection]);

  /** Render the sort indicator arrow next to sortable column headers */
  const renderSortIndicator = (key: string) => {
    if (sortKey !== key) {
      return <span className="ml-1 text-gray-300">&#8597;</span>;
    }
    return (
      <span className="ml-1">
        {sortDirection === "asc" ? "\u2191" : "\u2193"}
      </span>
    );
  };

  // Loading skeleton rows
  if (loading) {
    return (
      <div className="rounded-xl bg-white shadow-sm border border-gray-100 overflow-hidden">
        {onSearch && (
          <div className="p-4 border-b border-gray-100">
            <div className="h-10 w-full max-w-sm rounded-lg bg-gray-100 animate-pulse" />
          </div>
        )}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50/50">
                {columns.map((col) => (
                  <th
                    key={col.key}
                    className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-text-muted"
                  >
                    {col.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: 5 }).map((_, rowIdx) => (
                <tr key={rowIdx} className="border-b border-gray-50">
                  {columns.map((col) => (
                    <td key={col.key} className="px-4 py-3">
                      <div className="h-4 w-3/4 rounded bg-gray-100 animate-pulse" />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl bg-white shadow-sm border border-gray-100 overflow-hidden">
      {/* Search bar */}
      {onSearch && (
        <div className="p-4 border-b border-gray-100">
          <div className="relative w-full max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-muted" />
            <input
              type="text"
              placeholder={searchPlaceholder}
              onChange={(e) => onSearch(e.target.value)}
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-10 pr-4 text-sm text-text-dark placeholder-text-muted outline-none transition-colors focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/20"
            />
          </div>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-100 bg-gray-50/50">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={`px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-text-muted ${
                    col.sortable
                      ? "cursor-pointer select-none hover:text-text-dark transition-colors"
                      : ""
                  }`}
                  onClick={col.sortable ? () => handleSort(col.key) : undefined}
                >
                  <span className="inline-flex items-center">
                    {col.header}
                    {col.sortable && renderSortIndicator(col.key)}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sortedData.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-12 text-center text-sm text-text-muted"
                >
                  No data available
                </td>
              </tr>
            ) : (
              sortedData.map((item, rowIdx) => (
                <tr
                  key={rowIdx}
                  className={`border-b border-gray-50 transition-colors hover:bg-gray-50/50 ${
                    rowIdx % 2 === 1 ? "bg-gray-50/30" : ""
                  }`}
                >
                  {columns.map((col) => (
                    <td
                      key={col.key}
                      className="px-4 py-3 text-sm text-text-dark whitespace-nowrap"
                    >
                      {col.render
                        ? col.render(item)
                        : (getNestedValue(item, col.key) as React.ReactNode) ?? "—"}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pagination && pagination.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3">
          <p className="text-sm text-text-muted">
            Showing page{" "}
            <span className="font-medium text-text-dark">
              {pagination.page}
            </span>{" "}
            of{" "}
            <span className="font-medium text-text-dark">
              {pagination.totalPages}
            </span>{" "}
            &middot; {pagination.total} total
          </p>

          <div className="flex items-center gap-2">
            <button
              onClick={() => pagination.onPageChange(pagination.page - 1)}
              disabled={pagination.page <= 1}
              className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-sm font-medium text-text-dark transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
              Previous
            </button>
            <button
              onClick={() => pagination.onPageChange(pagination.page + 1)}
              disabled={pagination.page >= pagination.totalPages}
              className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-sm font-medium text-text-dark transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Next
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
