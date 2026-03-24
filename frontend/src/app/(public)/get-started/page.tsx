import Link from "next/link";
import { Package, Truck, ArrowRight } from "lucide-react";

const roles = [
  {
    role: "sender",
    href: "/get-started/sender",
    icon: Package,
    title: "I want to send packages",
    description:
      "Create a sender account to request deliveries, track packages in real-time, and pay securely with EcoCash, OneMoney, or card.",
    benefits: [
      "Request deliveries in under a minute",
      "Real-time GPS tracking",
      "Secure mobile & card payments",
      "Rate and review your drivers",
    ],
    cta: "Sign up as Sender",
    color: "accent",
  },
  {
    role: "driver",
    href: "/get-started/driver",
    icon: Truck,
    title: "I want to deliver packages",
    description:
      "Join our network of verified drivers. Earn money on your own schedule by delivering packages across the city.",
    benefits: [
      "Flexible hours — drive when you want",
      "Weekly payouts to EcoCash or bank",
      "In-app navigation & delivery management",
      "Grow your rating and earn more trips",
    ],
    cta: "Sign up as Driver",
    color: "primary",
  },
];

export default function GetStartedPage() {
  return (
    <main className="min-h-screen bg-gradient-to-br from-primary-light via-white to-white pt-28 pb-20 lg:pt-36 lg:pb-28">
      <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center mb-14">
          <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
            Get Started
          </p>
          <h1 className="text-3xl font-extrabold text-text-dark sm:text-4xl lg:text-5xl">
            How would you like to use{" "}
            <span className="text-primary">TumaGo</span>?
          </h1>
          <p className="mt-4 text-lg text-text-muted max-w-xl mx-auto">
            Choose your role to create your account. You can always reach out to
            us if you need to change later.
          </p>
        </div>

        {/* Role Cards */}
        <div className="grid gap-8 md:grid-cols-2">
          {roles.map((item) => (
            <Link
              key={item.role}
              href={item.href}
              className="group relative flex flex-col rounded-2xl border-2 border-gray-100 bg-white p-8 shadow-sm transition-all hover:shadow-xl hover:border-primary/30 hover:-translate-y-1"
            >
              <div className="mb-6 flex items-center gap-4">
                <div
                  className={`flex h-14 w-14 items-center justify-center rounded-xl ${
                    item.color === "accent"
                      ? "bg-accent/10 text-accent"
                      : "bg-primary/10 text-primary"
                  }`}
                >
                  <item.icon className="h-7 w-7" />
                </div>
              </div>
              <h2 className="text-xl font-bold text-text-dark mb-2">
                {item.title}
              </h2>
              <p className="text-text-muted text-sm leading-relaxed mb-6">
                {item.description}
              </p>
              <ul className="space-y-2.5 mb-8 flex-1">
                {item.benefits.map((benefit) => (
                  <li
                    key={benefit}
                    className="flex items-start gap-2.5 text-sm text-text-dark"
                  >
                    <svg
                      className={`mt-0.5 h-4 w-4 shrink-0 ${
                        item.color === "accent"
                          ? "text-accent"
                          : "text-primary"
                      }`}
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M5 13l4 4L19 7"
                      />
                    </svg>
                    {benefit}
                  </li>
                ))}
              </ul>
              <div
                className={`inline-flex items-center justify-center gap-2 rounded-xl px-6 py-3.5 text-sm font-semibold text-white transition-all ${
                  item.color === "accent"
                    ? "bg-accent group-hover:bg-orange-600"
                    : "bg-primary group-hover:bg-primary-dark"
                }`}
              >
                {item.cta}
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </div>
            </Link>
          ))}
        </div>

        {/* Already have account */}
        <p className="mt-10 text-center text-sm text-text-muted">
          Already have an account?{" "}
          <span className="text-primary font-medium">
            Open the TumaGo app to log in
          </span>
        </p>
      </div>
    </main>
  );
}
