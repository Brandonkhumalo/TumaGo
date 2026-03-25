import {
  Package,
  MapPin,
  Shield,
  Zap,
  Clock,
  Star,
  Truck,
  Users,
  ArrowRight,
  CheckCircle,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";

const steps = [
  {
    icon: Package,
    title: "Request a Delivery",
    description:
      "Open the TumaGo app, enter your pickup and drop-off locations, describe your package, and choose your payment method. It takes less than a minute.",
  },
  {
    icon: Zap,
    title: "Get Matched Instantly",
    description:
      "Our smart matching engine finds the nearest available, highest-rated driver and assigns them to your delivery within seconds.",
  },
  {
    icon: Truck,
    title: "Track & Receive",
    description:
      "Follow your package in real-time on a live map. Get notified when your driver picks up, is en route, and delivers your package.",
  },
];

const features = [
  {
    icon: MapPin,
    title: "Real-Time GPS Tracking",
    description:
      "Watch your package move on a live map from pickup to delivery. Know exactly where your driver is at every moment with our WebSocket-powered tracking.",
  },
  {
    icon: Shield,
    title: "Secure Payments",
    description:
      "Pay seamlessly with EcoCash, OneMoney, or card. Every transaction is encrypted and verified. No cash needed, no hidden fees.",
  },
  {
    icon: Star,
    title: "Verified & Rated Drivers",
    description:
      "Every TumaGo driver is verified with a valid license and vehicle inspection. Rate your driver after each delivery to help maintain quality.",
  },
  {
    icon: Clock,
    title: "Lightning-Fast Matching",
    description:
      "Our geo-based matching algorithm finds available drivers near your pickup location and assigns the best one in under 30 seconds.",
  },
];

const stats = [
  { value: "1,000+", label: "Deliveries Completed" },
  { value: "100+", label: "Active Drivers" },
  { value: "Harare", label: "Currently Operating" },
  { value: "< 25 min", label: "Avg. Delivery Time" },
];

export default function HomePage() {
  return (
    <main>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-light via-white to-white pt-28 pb-20 lg:pt-36 lg:pb-28">
        {/* Decorative Background Elements */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-primary/5" />
          <div className="absolute top-60 -left-20 h-60 w-60 rounded-full bg-accent/5" />
          <div className="absolute bottom-20 right-1/4 h-40 w-40 rounded-full bg-primary/5" />
        </div>

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid items-center gap-12 lg:grid-cols-2">
            {/* Left Content */}
            <div className="max-w-xl">
              <div className="mb-6 inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-1.5 text-sm font-medium text-primary">
                <Zap className="h-4 w-4" />
                Now available in Harare
              </div>
              <h1 className="text-4xl font-extrabold leading-tight tracking-tight text-text-dark sm:text-5xl lg:text-6xl">
                Fast, Reliable{" "}
                <span className="text-primary">Package Delivery</span>{" "}
                at Your Fingertips
              </h1>
              <p className="mt-6 text-lg leading-relaxed text-text-muted">
                TumaGo connects you with verified drivers for same-city
                package delivery across Zimbabwe. Request a pickup, track
                your package live, and pay securely with EcoCash, OneMoney,
                or card.
              </p>
              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <a
                  href="#"
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-7 py-3.5 text-base font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl hover:shadow-accent/30 hover:-translate-y-0.5"
                >
                  Download the App
                  <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs font-medium">
                    Coming Soon
                  </span>
                </a>
                <Link
                  href="/get-started"
                  className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-primary px-7 py-3.5 text-base font-semibold text-primary transition-all hover:bg-primary hover:text-white"
                >
                  Get Started
                </Link>
              </div>
              {/* Trust Badges */}
              <div className="mt-10 flex items-center gap-6 text-sm text-text-muted">
                <div className="flex items-center gap-2">
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  Verified Drivers
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  Insured Deliveries
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle className="h-5 w-5 text-green-500" />
                  24/7 Support
                </div>
              </div>
            </div>

            {/* Right Visual */}
            <div className="relative hidden lg:block">
              <div className="relative mx-auto w-full max-w-md">
                {/* Phone Mockup */}
                <div className="rounded-3xl bg-gradient-to-br from-primary to-primary-dark p-8 shadow-2xl shadow-primary/20">
                  <div className="rounded-2xl bg-white p-6 space-y-4">
                    <div className="flex items-center gap-3 mb-6">
                      <div className="h-10 w-10 rounded-full bg-primary-light flex items-center justify-center">
                        <Package className="h-5 w-5 text-primary" />
                      </div>
                      <div>
                        <p className="text-sm font-semibold text-text-dark">
                          New Delivery
                        </p>
                        <p className="text-xs text-text-muted">
                          Harare CBD to Borrowdale
                        </p>
                      </div>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center gap-3">
                        <div className="h-3 w-3 rounded-full bg-green-500" />
                        <div className="h-3 flex-1 rounded bg-gray-100" />
                      </div>
                      <div className="ml-1.5 h-6 border-l-2 border-dashed border-gray-200" />
                      <div className="flex items-center gap-3">
                        <div className="h-3 w-3 rounded-full bg-accent" />
                        <div className="h-3 flex-1 rounded bg-gray-100" />
                      </div>
                    </div>
                    <div className="mt-4 rounded-xl bg-primary-light p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-xs text-text-muted">
                            Estimated Fare
                          </p>
                          <p className="text-lg font-bold text-primary-dark">
                            $3.50 USD
                          </p>
                        </div>
                        <div className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white">
                          Confirm
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                {/* Floating Card - Driver Found */}
                <div className="absolute -right-8 top-8 rounded-xl bg-white p-4 shadow-xl shadow-gray-200/50 animate-bounce" style={{ animationDuration: "3s" }}>
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
                      <CheckCircle className="h-5 w-5 text-green-600" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-text-dark">
                        Driver Found!
                      </p>
                      <p className="text-xs text-text-muted">
                        2 min away
                      </p>
                    </div>
                  </div>
                </div>
                {/* Floating Card - Live Tracking */}
                <div className="absolute -left-8 bottom-16 rounded-xl bg-white p-4 shadow-xl shadow-gray-200/50">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-primary-light flex items-center justify-center">
                      <MapPin className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-text-dark">
                        Live Tracking
                      </p>
                      <div className="flex items-center gap-1">
                        <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
                        <p className="text-xs text-text-muted">
                          En route
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              How It Works
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Send a Package in Three Simple Steps
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              From request to delivery, TumaGo makes sending packages
              across the city effortless and transparent.
            </p>
          </div>
          <div className="grid gap-8 md:grid-cols-3">
            {steps.map((step, index) => (
              <div
                key={step.title}
                className="relative rounded-2xl border border-gray-100 bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:border-primary/20 hover:-translate-y-1"
              >
                <div className="mb-6 flex items-center gap-4">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary-light text-primary">
                    <step.icon className="h-7 w-7" />
                  </div>
                  <span className="text-5xl font-extrabold text-gray-100">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                </div>
                <h3 className="text-xl font-bold text-text-dark mb-3">
                  {step.title}
                </h3>
                <p className="text-text-muted leading-relaxed">
                  {step.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Features
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Everything You Need for Reliable Delivery
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              Built with cutting-edge technology to ensure your packages
              arrive safely, quickly, and affordably.
            </p>
          </div>
          <div className="grid gap-8 sm:grid-cols-2">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="group rounded-2xl bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:-translate-y-1"
              >
                <div className="mb-5 inline-flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <feature.icon className="h-7 w-7" />
                </div>
                <h3 className="text-xl font-bold text-text-dark mb-3">
                  {feature.title}
                </h3>
                <p className="text-text-muted leading-relaxed">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-20 lg:py-24 bg-gradient-to-r from-primary-dark to-primary text-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold sm:text-4xl">
              Trusted Across Zimbabwe
            </h2>
            <p className="mt-3 text-lg text-blue-100/80">
              Growing every day with drivers and senders who rely on us
            </p>
          </div>
          <div className="grid grid-cols-2 gap-8 lg:grid-cols-4">
            {stats.map((stat) => (
              <div key={stat.label} className="text-center">
                <p className="text-4xl font-extrabold sm:text-5xl">
                  {stat.value}
                </p>
                <p className="mt-2 text-sm font-medium text-blue-100/80">
                  {stat.label}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Driver CTA Section */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="rounded-3xl bg-gradient-to-br from-primary-light to-white p-10 sm:p-16 lg:flex lg:items-center lg:justify-between lg:gap-12">
            <div className="max-w-xl">
              <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
                Drive With TumaGo and Earn
              </h2>
              <p className="mt-4 text-lg text-text-muted leading-relaxed">
                Join our network of verified drivers and earn money on your
                own schedule. Get delivery requests in your area, navigate
                with ease, and get paid directly to your EcoCash or bank
                account.
              </p>
              <div className="mt-6 space-y-3">
                {[
                  "Flexible hours — drive when you want",
                  "Weekly payouts to EcoCash, OneMoney, or bank",
                  "In-app navigation and delivery management",
                  "Grow your rating and earn more trips",
                ].map((benefit) => (
                  <div
                    key={benefit}
                    className="flex items-center gap-3 text-text-dark"
                  >
                    <CheckCircle className="h-5 w-5 shrink-0 text-primary" />
                    <span className="text-sm">{benefit}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="mt-8 lg:mt-0 shrink-0">
              <div className="flex flex-col items-center gap-4 rounded-2xl bg-white p-8 shadow-lg">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary-light">
                  <Users className="h-10 w-10 text-primary" />
                </div>
                <p className="text-2xl font-bold text-text-dark">
                  100+ Drivers
                </p>
                <p className="text-sm text-text-muted">
                  and growing every week
                </p>
                <Link
                  href="/get-started"
                  className="w-full rounded-xl bg-primary px-6 py-3 text-center text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg"
                >
                  Get Started
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Tishanyq Branding */}
      <section className="py-10 bg-white border-t border-gray-100">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-xs uppercase tracking-widest text-text-muted mb-1">
            A product of
          </p>
          <a
            href="https://tishanyq.co.zw"
            target="_blank"
            rel="noopener noreferrer"
            className="text-lg font-bold text-text-dark hover:text-primary transition-colors"
          >
            Tishanyq Digital Pvt Ltd
          </a>
        </div>
      </section>

      {/* Final CTA Section */}
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
          <div className="flex justify-center mb-6">
            <Image
              src="/tuma_go_logo.png"
              alt="TumaGo"
              width={64}
              height={64}
            />
          </div>
          <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
            Ready to Send a Package?
          </h2>
          <p className="mt-4 text-lg text-text-muted max-w-lg mx-auto">
            Download TumaGo today and experience the fastest, most
            reliable package delivery service in Zimbabwe. Your first
            delivery is on us.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:justify-center">
            <a
              href="#"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-8 py-4 text-base font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl hover:-translate-y-0.5"
            >
              <svg
                viewBox="0 0 24 24"
                fill="currentColor"
                className="h-5 w-5"
              >
                <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.198l2.807 1.626a1 1 0 0 1 0 1.73l-2.808 1.626L15.206 12l2.492-2.491zM5.864 2.658L16.802 8.99l-2.303 2.303-8.635-8.635z" />
              </svg>
              Google Play
              <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs font-medium">
                Coming Soon
              </span>
            </a>
            <a
              href="#"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-text-dark px-8 py-4 text-base font-semibold text-white shadow-lg transition-all hover:bg-gray-800 hover:shadow-xl hover:-translate-y-0.5"
            >
              <svg
                viewBox="0 0 24 24"
                fill="currentColor"
                className="h-5 w-5"
              >
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
              </svg>
              App Store
              <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs font-medium">
                Coming Soon
              </span>
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
