import {
  Code,
  Key,
  Package,
  MapPin,
  XCircle,
  List,
  Wallet,
  Webhook,
  ArrowRight,
  Shield,
  Zap,
} from "lucide-react";
import Link from "next/link";

const baseUrl = "https://api.tumago.co.zw/api/v1";

const endpoints = [
  {
    method: "POST",
    path: "/partner/delivery/request/",
    title: "Request a Delivery",
    description:
      "Create a new delivery request. The fare is automatically deducted from your partner balance.",
    body: `{
  "origin_lat": -17.8292,
  "origin_lng": 31.0522,
  "destination_lat": -17.7936,
  "destination_lng": 31.0492,
  "vehicle": "car",
  "fare": 5.00,
  "payment_method": "account",
  "partner_reference": "ORDER-123",
  "pickup_contact": "0771234567",
  "dropoff_contact": "0779876543",
  "package_description": "Small electronics parcel"
}`,
    response: `{
  "id": "uuid",
  "partner_reference": "ORDER-123",
  "status": "matching",
  "trip_id": "uuid"
}`,
    notes: [
      'Vehicle options: "car", "motorcycle", "bicycle"',
      "Fare must be > 0 and within your balance",
      "partner_reference is your internal order ID for tracking",
    ],
    icon: Package,
  },
  {
    method: "GET",
    path: "/partner/delivery/{id}/status/",
    title: "Get Delivery Status",
    description:
      "Poll the current status of a delivery. Includes driver info, vehicle details, and live GPS location once a driver is assigned.",
    body: null,
    response: `{
  "id": "uuid",
  "partner_reference": "ORDER-123",
  "status": "driver_assigned",
  "origin": { "lat": -17.8292, "lng": 31.0522 },
  "destination": { "lat": -17.7936, "lng": 31.0492 },
  "fare": 5.00,
  "driver": {
    "name": "John Moyo",
    "rating": 4.8,
    "phone": "0771234567"
  },
  "vehicle": {
    "type": "car",
    "name": "Toyota Vitz",
    "color": "Silver",
    "number_plate": "ABC 1234",
    "model": "2019"
  },
  "driver_location": { "lat": -17.825, "lng": 31.051 }
}`,
    notes: [
      "Statuses: pending → matching → driver_assigned → picked_up → delivered / cancelled",
      "driver_location is only available when a driver is assigned and online",
      "Poll this endpoint every 5-10 seconds for real-time updates",
    ],
    icon: MapPin,
  },
  {
    method: "POST",
    path: "/partner/delivery/{id}/cancel/",
    title: "Cancel a Delivery",
    description:
      "Cancel an active delivery. The fare is refunded to your partner balance. Cannot cancel already delivered or cancelled deliveries.",
    body: null,
    response: `{
  "detail": "Delivery cancelled.",
  "status": "cancelled"
}`,
    notes: [
      "Full fare is refunded on cancellation",
      "If a driver was assigned, they are freed and notified",
      'Cannot cancel if status is "delivered" or "cancelled"',
    ],
    icon: XCircle,
  },
  {
    method: "GET",
    path: "/partner/deliveries/",
    title: "List Deliveries",
    description:
      "Retrieve a paginated list of all your delivery requests, sorted by most recent.",
    body: null,
    response: `{
  "count": 42,
  "page": 1,
  "page_size": 20,
  "results": [
    {
      "id": "uuid",
      "partner_reference": "ORDER-123",
      "status": "delivered",
      "created_at": "2026-03-20T10:30:00Z",
      "delivery_id": "uuid"
    }
  ]
}`,
    notes: [
      "Query params: ?page=1&page_size=20",
      "Max page_size is 100",
    ],
    icon: List,
  },
  {
    method: "GET",
    path: "/partner/balance/",
    title: "Check Balance",
    description:
      "View your current account balance, setup fee status, and commission rate.",
    body: null,
    response: `{
  "balance": 150.00,
  "setup_fee_paid": true,
  "commission_rate": 20.00
}`,
    notes: [
      "Balance decreases with each delivery request",
      "Commission rate is the platform percentage kept per delivery",
    ],
    icon: Wallet,
  },
];

const webhookEvents = [
  {
    event: "driver_assigned",
    description: "A driver has been matched and assigned to your delivery",
  },
  {
    event: "picked_up",
    description: "The driver has picked up the package",
  },
  {
    event: "delivered",
    description: "The package has been delivered to the destination",
  },
  {
    event: "cancelled",
    description: "The delivery was cancelled",
  },
  {
    event: "location_update",
    description:
      "Periodic driver GPS updates (~30s intervals during active delivery)",
  },
];

function MethodBadge({ method }: { method: string }) {
  const colors: Record<string, string> = {
    GET: "bg-green-100 text-green-700",
    POST: "bg-blue-100 text-blue-700",
    PUT: "bg-amber-100 text-amber-700",
    DELETE: "bg-red-100 text-red-700",
  };
  return (
    <span
      className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-bold uppercase tracking-wider ${colors[method] || "bg-gray-100 text-gray-700"}`}
    >
      {method}
    </span>
  );
}

export default function PartnerDocsPage() {
  return (
    <main>
      {/* Hero */}
      <section className="relative bg-gradient-to-br from-primary-dark to-primary pt-28 pb-16 lg:pt-36 lg:pb-20">
        <div className="relative mx-auto max-w-4xl px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium text-blue-200 mb-6">
            <Code className="h-4 w-4" />
            API Reference
          </div>
          <h1 className="text-4xl font-extrabold text-white sm:text-5xl">
            Partner API Documentation
          </h1>
          <p className="mt-4 text-lg text-blue-100/90 max-w-2xl mx-auto">
            Integrate TumaGo deliveries directly into your systems. Create
            deliveries, track packages in real-time, and receive webhooks —
            all via our REST API.
          </p>
          <div className="mt-6">
            <Link
              href="/partner#register"
              className="inline-flex items-center gap-2 rounded-xl bg-accent px-6 py-3 text-sm font-semibold text-white shadow-lg transition-all hover:bg-orange-600 hover:-translate-y-0.5"
            >
              Get Your API Key
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* Quick Start */}
      <section className="py-16 bg-white">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold text-text-dark mb-8">
            Quick Start
          </h2>
          <div className="grid gap-6 md:grid-cols-3 mb-10">
            {[
              {
                icon: Shield,
                title: "Authentication",
                text: "Include your API key in every request via the X-API-Key header.",
              },
              {
                icon: Zap,
                title: "Base URL",
                text: baseUrl,
              },
              {
                icon: Key,
                title: "Rate Limit",
                text: "60 requests/minute with a burst of 20. Contact us to increase.",
              },
            ].map((item) => (
              <div
                key={item.title}
                className="rounded-xl border border-gray-100 p-5"
              >
                <item.icon className="h-6 w-6 text-primary mb-3" />
                <h3 className="text-sm font-bold text-text-dark mb-1">
                  {item.title}
                </h3>
                <p className="text-sm text-text-muted break-all">
                  {item.text}
                </p>
              </div>
            ))}
          </div>

          {/* Auth example */}
          <div className="rounded-xl bg-gray-900 p-6 text-sm">
            <p className="text-gray-400 text-xs mb-3 uppercase tracking-wider font-semibold">
              Example Request
            </p>
            <pre className="text-green-400 overflow-x-auto">
              <code>{`curl -X POST ${baseUrl}/partner/delivery/request/ \\
  -H "Content-Type: application/json" \\
  -H "X-API-Key: tg_live_your_api_key_here" \\
  -d '{
    "origin_lat": -17.8292,
    "origin_lng": 31.0522,
    "destination_lat": -17.7936,
    "destination_lng": 31.0492,
    "vehicle": "car",
    "fare": 5.00,
    "partner_reference": "ORDER-123"
  }'`}</code>
            </pre>
          </div>
        </div>
      </section>

      {/* Endpoints */}
      <section className="py-16 bg-primary-light">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold text-text-dark mb-2">
            API Endpoints
          </h2>
          <p className="text-text-muted mb-10">
            All endpoints require the{" "}
            <code className="bg-gray-200 px-1.5 py-0.5 rounded text-sm">
              X-API-Key
            </code>{" "}
            header.
          </p>

          <div className="space-y-8">
            {endpoints.map((ep) => (
              <div
                key={ep.path}
                id={ep.path.replace(/[^a-z]/g, "-")}
                className="rounded-2xl bg-white border border-gray-100 overflow-hidden shadow-sm"
              >
                {/* Header */}
                <div className="px-6 py-4 border-b border-gray-100 flex items-center gap-3">
                  <ep.icon className="h-5 w-5 text-primary shrink-0" />
                  <div className="flex items-center gap-3 flex-wrap">
                    <MethodBadge method={ep.method} />
                    <code className="text-sm font-mono text-text-dark">
                      {ep.path}
                    </code>
                  </div>
                </div>

                <div className="px-6 py-5 space-y-4">
                  <div>
                    <h3 className="text-lg font-bold text-text-dark">
                      {ep.title}
                    </h3>
                    <p className="text-sm text-text-muted mt-1">
                      {ep.description}
                    </p>
                  </div>

                  {/* Request Body */}
                  {ep.body && (
                    <div>
                      <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-2">
                        Request Body
                      </p>
                      <pre className="rounded-lg bg-gray-900 p-4 text-sm text-green-400 overflow-x-auto">
                        <code>{ep.body}</code>
                      </pre>
                    </div>
                  )}

                  {/* Response */}
                  <div>
                    <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-2">
                      Response
                    </p>
                    <pre className="rounded-lg bg-gray-900 p-4 text-sm text-blue-300 overflow-x-auto">
                      <code>{ep.response}</code>
                    </pre>
                  </div>

                  {/* Notes */}
                  {ep.notes.length > 0 && (
                    <div className="rounded-lg bg-primary-light border border-primary/10 p-4">
                      <p className="text-xs font-semibold text-primary uppercase tracking-wider mb-2">
                        Notes
                      </p>
                      <ul className="space-y-1">
                        {ep.notes.map((note, i) => (
                          <li
                            key={i}
                            className="text-sm text-text-muted flex items-start gap-2"
                          >
                            <span className="text-primary mt-1">•</span>
                            {note}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Webhooks */}
      <section className="py-16 bg-white">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3 mb-8">
            <Webhook className="h-6 w-6 text-primary" />
            <h2 className="text-2xl font-bold text-text-dark">Webhooks</h2>
          </div>
          <p className="text-text-muted mb-6">
            Configure a webhook URL in your partner dashboard to receive
            real-time delivery updates. We send HMAC-SHA256 signed POST
            requests to your URL.
          </p>

          {/* Webhook headers */}
          <div className="rounded-xl bg-gray-900 p-5 mb-8">
            <p className="text-gray-400 text-xs mb-3 uppercase tracking-wider font-semibold">
              Webhook Headers
            </p>
            <pre className="text-green-400 text-sm overflow-x-auto">
              <code>{`X-TumaGo-Signature: <HMAC-SHA256 hex digest>
X-TumaGo-Event: driver_assigned
Content-Type: application/json`}</code>
            </pre>
            <p className="text-gray-500 text-xs mt-3">
              Verify the signature using your API Secret to ensure the
              request is authentic.
            </p>
          </div>

          {/* Events table */}
          <div className="rounded-xl border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="text-left px-5 py-3 text-xs font-semibold text-text-muted uppercase tracking-wider">
                    Event
                  </th>
                  <th className="text-left px-5 py-3 text-xs font-semibold text-text-muted uppercase tracking-wider">
                    Description
                  </th>
                </tr>
              </thead>
              <tbody>
                {webhookEvents.map((evt) => (
                  <tr
                    key={evt.event}
                    className="border-b border-gray-100 last:border-0"
                  >
                    <td className="px-5 py-3">
                      <code className="rounded bg-gray-100 px-2 py-0.5 text-xs font-mono text-primary-dark">
                        {evt.event}
                      </code>
                    </td>
                    <td className="px-5 py-3 text-text-muted">
                      {evt.description}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {/* Error Codes */}
      <section className="py-16 bg-primary-light">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl font-bold text-text-dark mb-6">
            Error Codes
          </h2>
          <div className="rounded-xl border border-gray-200 bg-white overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="text-left px-5 py-3 text-xs font-semibold text-text-muted uppercase tracking-wider">
                    Status
                  </th>
                  <th className="text-left px-5 py-3 text-xs font-semibold text-text-muted uppercase tracking-wider">
                    Meaning
                  </th>
                </tr>
              </thead>
              <tbody>
                {[
                  ["400", "Bad request — missing or invalid fields"],
                  ["401", "Unauthorized — invalid or missing API key"],
                  ["402", "Insufficient balance for this delivery"],
                  ["403", "Forbidden — partner account is deactivated"],
                  ["404", "Delivery not found"],
                  ["429", "Rate limit exceeded — slow down requests"],
                  ["502", "Payment gateway error — retry shortly"],
                ].map(([code, meaning]) => (
                  <tr
                    key={code}
                    className="border-b border-gray-100 last:border-0"
                  >
                    <td className="px-5 py-3">
                      <code className="rounded bg-red-50 px-2 py-0.5 text-xs font-mono font-bold text-red-700">
                        {code}
                      </code>
                    </td>
                    <td className="px-5 py-3 text-text-muted">{meaning}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 bg-white">
        <div className="mx-auto max-w-2xl px-4 text-center">
          <h2 className="text-2xl font-bold text-text-dark">
            Ready to Integrate?
          </h2>
          <p className="mt-3 text-text-muted">
            Register your business, pay the one-time $15 setup fee, and
            start making API calls in minutes.
          </p>
          <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
            <Link
              href="/partner#register"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-6 py-3 text-sm font-semibold text-white shadow-lg transition-all hover:bg-orange-600 hover:-translate-y-0.5"
            >
              Register Now
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              href="/partner/dashboard"
              className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-primary px-6 py-3 text-sm font-semibold text-primary transition-all hover:bg-primary hover:text-white"
            >
              Partner Dashboard
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
