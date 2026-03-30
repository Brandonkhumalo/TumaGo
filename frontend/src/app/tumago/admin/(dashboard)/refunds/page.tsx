"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { adminAPI } from "@/lib/api";

interface RefundRequest {
  id: string;
  amount: number;
  reason: string;
  status: string;
  admin_notes: string;
  created_at: string;
  reviewed_at: string | null;
  driver_id: string;
  driver_name: string;
  driver_email: string;
  delivery_id: string;
  delivery_fare: number;
  payment_method: string;
}

interface RefundMetrics {
  pending: number;
  approved: number;
  denied: number;
  total: number;
  total_refunded: number;
}

export default function RefundRequestsPage() {
  const { token } = useAuth();
  const [requests, setRequests] = useState<RefundRequest[]>([]);
  const [metrics, setMetrics] = useState<RefundMetrics | null>(null);
  const [statusFilter, setStatusFilter] = useState("pending");
  const [loading, setLoading] = useState(true);
  const [reviewingId, setReviewingId] = useState<string | null>(null);
  const [adminNotes, setAdminNotes] = useState("");

  useEffect(() => {
    if (token) {
      loadData();
    }
  }, [token, statusFilter]);

  async function loadData() {
    if (!token) return;
    setLoading(true);
    try {
      const [reqData, metricsData] = await Promise.all([
        adminAPI.refundRequests(token, `status=${statusFilter}`),
        adminAPI.refundMetrics(token),
      ]);
      setRequests(reqData.results || []);
      setMetrics(metricsData);
    } catch (err) {
      console.error("Failed to load refund data:", err);
    } finally {
      setLoading(false);
    }
  }

  async function handleReview(refundId: string, action: "approve" | "deny") {
    if (!token) return;
    try {
      await adminAPI.reviewRefund(token, refundId, {
        action,
        admin_notes: adminNotes,
      });
      setReviewingId(null);
      setAdminNotes("");
      loadData();
    } catch (err) {
      console.error("Review failed:", err);
    }
  }

  const statusColors: Record<string, string> = {
    pending: "bg-yellow-100 text-yellow-800",
    approved: "bg-green-100 text-green-800",
    denied: "bg-red-100 text-red-800",
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">
        Commission Refund Requests
      </h1>

      {/* Metrics Cards */}
      {metrics && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard
            label="Pending"
            value={metrics.pending}
            color="text-yellow-600"
          />
          <MetricCard
            label="Approved"
            value={metrics.approved}
            color="text-green-600"
          />
          <MetricCard
            label="Denied"
            value={metrics.denied}
            color="text-red-600"
          />
          <MetricCard
            label="Total Refunded"
            value={`$${metrics.total_refunded.toFixed(2)}`}
            color="text-blue-600"
          />
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex gap-2">
        {["pending", "approved", "denied"].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              statusFilter === s
                ? "bg-blue-600 text-white"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            {s.charAt(0).toUpperCase() + s.slice(1)}
            {metrics && (
              <span className="ml-1 opacity-75">
                ({metrics[s as keyof RefundMetrics]})
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Requests Table */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin h-8 w-8 border-4 border-blue-500 border-t-transparent rounded-full" />
        </div>
      ) : requests.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          No {statusFilter} refund requests
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="text-left px-4 py-3 font-medium">Driver</th>
                <th className="text-left px-4 py-3 font-medium">Amount</th>
                <th className="text-left px-4 py-3 font-medium">
                  Delivery Fare
                </th>
                <th className="text-left px-4 py-3 font-medium">Reason</th>
                <th className="text-left px-4 py-3 font-medium">Date</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                {statusFilter === "pending" && (
                  <th className="text-left px-4 py-3 font-medium">Actions</th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {requests.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900">
                      {r.driver_name}
                    </div>
                    <div className="text-xs text-gray-500">
                      {r.driver_email}
                    </div>
                  </td>
                  <td className="px-4 py-3 font-medium">
                    ${r.amount.toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    ${r.delivery_fare.toFixed(2)}{" "}
                    <span className="text-xs text-gray-400">
                      ({r.payment_method})
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-600 max-w-xs truncate">
                    {r.reason}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {r.created_at
                      ? new Date(r.created_at).toLocaleDateString()
                      : ""}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        statusColors[r.status] || ""
                      }`}
                    >
                      {r.status}
                    </span>
                  </td>
                  {statusFilter === "pending" && (
                    <td className="px-4 py-3">
                      {reviewingId === r.id ? (
                        <div className="space-y-2">
                          <input
                            type="text"
                            placeholder="Admin notes (optional)"
                            value={adminNotes}
                            onChange={(e) => setAdminNotes(e.target.value)}
                            className="w-full px-2 py-1 border rounded text-xs"
                          />
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleReview(r.id, "approve")}
                              className="px-3 py-1 bg-green-600 text-white text-xs rounded hover:bg-green-700"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() => handleReview(r.id, "deny")}
                              className="px-3 py-1 bg-red-600 text-white text-xs rounded hover:bg-red-700"
                            >
                              Deny
                            </button>
                            <button
                              onClick={() => {
                                setReviewingId(null);
                                setAdminNotes("");
                              }}
                              className="px-3 py-1 bg-gray-200 text-gray-600 text-xs rounded"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <button
                          onClick={() => setReviewingId(r.id)}
                          className="px-3 py-1 bg-blue-600 text-white text-xs rounded hover:bg-blue-700"
                        >
                          Review
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function MetricCard({
  label,
  value,
  color,
}: {
  label: string;
  value: number | string;
  color: string;
}) {
  return (
    <div className="bg-white rounded-xl shadow-sm p-4">
      <div className="text-sm text-gray-500">{label}</div>
      <div className={`text-2xl font-bold mt-1 ${color}`}>{value}</div>
    </div>
  );
}
