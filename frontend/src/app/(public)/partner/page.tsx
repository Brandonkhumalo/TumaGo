"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import {
  Handshake,
  Code,
  HeadphonesIcon,
  TrendingUp,
  MapPin,
  ArrowRight,
  CheckCircle,
  Building2,
  ShoppingBag,
  Pill,
  UtensilsCrossed,
  Package,
  BarChart3,
  Zap,
  Shield,
  DollarSign,
  Wallet,
  BarChart,
  Users,
  Smartphone,
  Eye,
  EyeOff,
  Loader2,
} from "lucide-react";
import { partnerAPI } from "@/lib/api";

const benefits = [
  {
    icon: Code,
    title: "API Integration",
    description:
      "Access our RESTful API to create deliveries, track packages, and receive real-time webhooks — all programmatically.",
  },
  {
    icon: HeadphonesIcon,
    title: "Dedicated Support",
    description:
      "Get a dedicated account manager who understands your business. Priority support and custom onboarding.",
  },
  {
    icon: TrendingUp,
    title: "Volume Pricing",
    description:
      "Benefit from competitive tiered pricing as your delivery volume grows. No hidden fees, transparent billing.",
  },
  {
    icon: MapPin,
    title: "Real-Time Tracking",
    description:
      "Give your customers full visibility with live GPS tracking on every delivery. Automated status notifications via webhook.",
  },
  {
    icon: BarChart3,
    title: "Analytics Dashboard",
    description:
      "Access detailed analytics on delivery performance, driver ratings, delivery times, and costs.",
  },
  {
    icon: Shield,
    title: "Insured Deliveries",
    description:
      "Every partner delivery is covered by our insurance policy. Ship with confidence knowing your packages are protected.",
  },
];

const industries = [
  {
    icon: ShoppingBag,
    name: "E-Commerce",
    description: "Same-day delivery for online stores",
  },
  {
    icon: UtensilsCrossed,
    name: "Food & Restaurants",
    description: "Fast delivery for meals and groceries",
  },
  {
    icon: Pill,
    name: "Pharmacies",
    description: "Reliable medicine delivery to patients",
  },
  {
    icon: Building2,
    name: "Corporate",
    description: "Document and parcel delivery for offices",
  },
  {
    icon: Package,
    name: "Retail",
    description: "Customer deliveries for brick-and-mortar stores",
  },
  {
    icon: Zap,
    name: "On-Demand",
    description: "Any business needing fast, flexible delivery",
  },
];

type Step = "register" | "payment" | "polling" | "done";

export default function PartnerPage() {
  const [step, setStep] = useState<Step>("register");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Registration form
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
    description: "",
    address: "",
    city: "",
    contact_person_name: "",
    contact_person_role: "",
  });

  // Payment form
  const [partnerId, setPartnerId] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("");
  const [paymentPhone, setPaymentPhone] = useState("");
  const [redirectUrl, setRedirectUrl] = useState("");
  const [pollInterval, setPollInterval] = useState<ReturnType<typeof setInterval> | null>(null);

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await partnerAPI.register(formData);
      setPartnerId(res.partner_id);
      setPaymentPhone(formData.phone);
      setStep("payment");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Registration failed.");
    } finally {
      setLoading(false);
    }
  };

  const handlePaySetup = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await partnerAPI.paySetup({
        partner_id: partnerId,
        email: formData.email,
        payment_method: paymentMethod,
        phone: paymentPhone,
      });
      if (res.redirect_url) {
        setRedirectUrl(res.redirect_url);
      }
      // Start polling for payment status
      setStep("polling");
      const interval = setInterval(async () => {
        try {
          const status = await partnerAPI.paySetupStatus(
            partnerId,
            formData.email
          );
          if (status.paid) {
            clearInterval(interval);
            setPollInterval(null);
            setStep("done");
          }
        } catch {
          // Keep polling
        }
      }, 5000);
      setPollInterval(interval);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Payment initiation failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-dark to-primary pt-28 pb-20 lg:pt-36 lg:pb-24">
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-white/5" />
          <div className="absolute bottom-0 left-1/3 h-60 w-60 rounded-full bg-white/5" />
        </div>
        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium text-blue-200 mb-6">
            <Handshake className="h-4 w-4" />
            For Businesses
          </div>
          <h1 className="text-4xl font-extrabold text-white sm:text-5xl lg:text-6xl">
            Partner With TumaGo
          </h1>
          <p className="mt-6 text-lg text-blue-100/90 max-w-2xl mx-auto leading-relaxed">
            Register your business, get your API key, and give your team
            access to reliable last-mile delivery — all in minutes.
            Partnership is for the <strong>sender side</strong> of your
            business.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:justify-center">
            <a
              href="#register"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-8 py-4 text-base font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl hover:-translate-y-0.5"
            >
              Register Now
              <ArrowRight className="h-5 w-5" />
            </a>
            <Link
              href="/partner/docs"
              className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-white/30 px-8 py-4 text-base font-semibold text-white transition-all hover:bg-white/10"
            >
              <Code className="h-5 w-5" />
              API Docs
            </Link>
          </div>
        </div>
      </section>

      {/* What You Get Section */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              What You Get
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Register, Pay, Start Delivering
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              No enquiry forms or waiting. Sign up, pay the one-time setup
              fee, and you&apos;re live.
            </p>
          </div>
          <div className="grid gap-8 md:grid-cols-3">
            {[
              {
                icon: DollarSign,
                number: "01",
                title: "Register & Pay $15",
                description:
                  "Create your business account and pay the one-time $15 setup fee via EcoCash, OneMoney, or card.",
              },
              {
                icon: Code,
                number: "02",
                title: "Get Your API Key",
                description:
                  "Instantly receive your API key and secret. Integrate TumaGo deliveries into your systems via our REST API.",
              },
              {
                icon: Smartphone,
                number: "03",
                title: "Create 10 App Logins",
                description:
                  "Create up to 10 login credentials for your staff devices. Each device can request deliveries via the TumaGo sender app.",
              },
            ].map((item) => (
              <div
                key={item.number}
                className="relative rounded-2xl border border-gray-100 bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:border-primary/20 hover:-translate-y-1"
              >
                <div className="mb-6 flex items-center gap-4">
                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary-light text-primary">
                    <item.icon className="h-7 w-7" />
                  </div>
                  <span className="text-5xl font-extrabold text-gray-100">
                    {item.number}
                  </span>
                </div>
                <h3 className="text-xl font-bold text-text-dark mb-3">
                  {item.title}
                </h3>
                <p className="text-text-muted leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Industries Section */}
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Industries We Serve
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Delivery Solutions for Every Business
            </h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {industries.map((industry) => (
              <div
                key={industry.name}
                className="group flex items-start gap-4 rounded-2xl bg-white border border-gray-100 p-6 transition-all hover:shadow-lg hover:border-primary/20 hover:-translate-y-1"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <industry.icon className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="font-bold text-text-dark">{industry.name}</h3>
                  <p className="mt-1 text-sm text-text-muted">
                    {industry.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Partner Benefits
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Why Businesses Choose TumaGo
            </h2>
          </div>
          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {benefits.map((benefit) => (
              <div
                key={benefit.title}
                className="group rounded-2xl bg-primary-light p-8 transition-all hover:shadow-lg hover:-translate-y-1"
              >
                <div className="mb-5 inline-flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <benefit.icon className="h-7 w-7" />
                </div>
                <h3 className="text-lg font-bold text-text-dark mb-3">
                  {benefit.title}
                </h3>
                <p className="text-sm text-text-muted leading-relaxed">
                  {benefit.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section
        id="pricing"
        className="py-20 lg:py-24 bg-gradient-to-r from-primary-dark to-primary text-white"
      >
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold sm:text-4xl">
              Simple, Transparent Pricing
            </h2>
            <p className="mt-4 text-lg text-blue-100/80 max-w-2xl mx-auto">
              One-time setup fee. No monthly subscriptions. Pay only for
              deliveries you use.
            </p>
          </div>

          {/* Pricing Cards */}
          <div className="grid gap-8 md:grid-cols-2 max-w-3xl mx-auto mb-16">
            {/* Setup Fee */}
            <div className="rounded-2xl bg-white p-8 text-center shadow-xl">
              <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                <DollarSign className="h-7 w-7 text-primary" />
              </div>
              <p className="text-4xl font-extrabold text-primary-dark">$15</p>
              <p className="mt-1 text-sm font-semibold text-text-dark uppercase tracking-wider">
                One-Time Setup Fee
              </p>
              <p className="mt-3 text-sm text-text-muted leading-relaxed">
                Register and pay once. Get your API key + 10 device logins
                included.
              </p>
              <div className="mt-4 space-y-2 text-left">
                {[
                  "API Key & Secret",
                  "10 app login credentials",
                  "Partner dashboard access",
                  "Webhook integration",
                ].map((item) => (
                  <div key={item} className="flex items-center gap-2 text-sm text-text-dark">
                    <CheckCircle className="h-4 w-4 shrink-0 text-primary" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Extra Devices */}
            <div className="rounded-2xl bg-white p-8 text-center shadow-xl">
              <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-accent/10">
                <Smartphone className="h-7 w-7 text-accent" />
              </div>
              <p className="text-4xl font-extrabold text-primary-dark">$5</p>
              <p className="mt-1 text-sm font-semibold text-text-dark uppercase tracking-wider">
                Per 7 Extra Devices
              </p>
              <p className="mt-3 text-sm text-text-muted leading-relaxed">
                Need more than 10 devices? Purchase additional login
                credentials in packs of 7.
              </p>
              <div className="mt-4 space-y-2 text-left">
                {[
                  "7 additional app logins",
                  "Deducted from partner balance",
                  "Create credentials instantly",
                  "Buy as many packs as needed",
                ].map((item) => (
                  <div key={item} className="flex items-center gap-2 text-sm text-text-dark">
                    <CheckCircle className="h-4 w-4 shrink-0 text-accent" />
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Commission Structure */}
          <div>
            <h3 className="text-center text-xl font-bold mb-10">
              Delivery Commission
            </h3>
            <div className="grid gap-8 md:grid-cols-2 max-w-3xl mx-auto">
              <div className="rounded-2xl bg-white/10 backdrop-blur p-8">
                <div className="flex items-center gap-3 mb-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/20">
                    <DollarSign className="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <p className="text-2xl font-extrabold">20%</p>
                    <p className="text-xs text-blue-100/80 uppercase tracking-wider font-semibold">
                      Platform Fee
                    </p>
                  </div>
                </div>
                <p className="text-sm text-blue-100/80 leading-relaxed">
                  We keep 20% of each delivery fare to maintain the platform,
                  driver network, and support.
                </p>
              </div>
              <div className="rounded-2xl bg-white/10 backdrop-blur p-8">
                <div className="flex items-center gap-3 mb-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/20">
                    <Users className="h-5 w-5 text-white" />
                  </div>
                  <div>
                    <p className="text-2xl font-extrabold">80%</p>
                    <p className="text-xs text-blue-100/80 uppercase tracking-wider font-semibold">
                      To Driver
                    </p>
                  </div>
                </div>
                <p className="text-sm text-blue-100/80 leading-relaxed">
                  The remaining 80% goes directly to the driver who completes
                  your delivery.
                </p>
              </div>
            </div>
            <div className="mt-8 mx-auto max-w-xl rounded-2xl bg-white/10 backdrop-blur px-6 py-5 text-center">
              <p className="text-sm font-semibold text-blue-100/80 mb-2 uppercase tracking-wider">
                Example
              </p>
              <p className="text-lg font-bold">
                $10.00 delivery{" "}
                <span className="text-blue-200 mx-2">&rarr;</span> $2.00
                platform fee{" "}
                <span className="text-blue-200 mx-2">&rarr;</span> $8.00 to
                driver
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* How Billing Works */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Billing
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Balance-Based Delivery Payments
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              Deposit funds, request deliveries, and only pay for what you use.
            </p>
          </div>
          <div className="grid gap-8 md:grid-cols-3">
            {[
              {
                step: "01",
                icon: Wallet,
                title: "Deposit Funds",
                description:
                  "Add money to your TumaGo partner account via bank transfer or mobile money. Available instantly.",
              },
              {
                step: "02",
                icon: Package,
                title: "Request Deliveries",
                description:
                  "Each delivery fare is automatically deducted from your balance. No invoices, no delays.",
              },
              {
                step: "03",
                icon: BarChart,
                title: "Track Everything",
                description:
                  "View your balance, transaction history, and delivery costs in real-time via API and dashboard.",
              },
            ].map((item) => (
              <div
                key={item.step}
                className="rounded-2xl border border-gray-100 bg-white p-8 text-center transition-all hover:shadow-lg hover:border-primary/20"
              >
                <span className="text-5xl font-extrabold text-primary/10">
                  {item.step}
                </span>
                <div className="mx-auto mt-4 mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary-light">
                  <item.icon className="h-6 w-6 text-primary" />
                </div>
                <h4 className="text-lg font-bold text-text-dark">
                  {item.title}
                </h4>
                <p className="mt-3 text-sm text-text-muted leading-relaxed">
                  {item.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Registration Section */}
      <section id="register" className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-2 lg:items-start">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
                Get Started
              </p>
              <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
                Register Your Business
              </h2>
              <p className="mt-4 text-text-muted leading-relaxed">
                Create your partner account in minutes. Pay the one-time $15
                setup fee and start using TumaGo for your business deliveries
                immediately.
              </p>
              <div className="mt-8 space-y-6">
                {[
                  {
                    title: "Client-Side Only",
                    desc: "Partnership covers the sending side of your business — request and track deliveries",
                  },
                  {
                    title: "10 Device Logins Included",
                    desc: "Create login credentials for up to 10 staff devices at no extra cost",
                  },
                  {
                    title: "Pay-As-You-Go",
                    desc: "Deposit funds and only pay for deliveries you use",
                  },
                  {
                    title: "Instant API Access",
                    desc: "Get your API key and secret immediately after payment",
                  },
                ].map((item) => (
                  <div key={item.title} className="flex items-start gap-4">
                    <CheckCircle className="h-6 w-6 shrink-0 text-primary mt-0.5" />
                    <div>
                      <p className="font-semibold text-text-dark">
                        {item.title}
                      </p>
                      <p className="text-sm text-text-muted">{item.desc}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Already have an account? */}
              <div className="mt-10 rounded-xl border border-primary/20 bg-white p-5">
                <p className="text-sm font-semibold text-text-dark mb-1">
                  Already registered?
                </p>
                <p className="text-sm text-text-muted mb-3">
                  Log in to your partner dashboard to manage devices, view
                  your API key, and track deliveries.
                </p>
                <Link
                  href="/partner/dashboard"
                  className="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:text-primary-dark transition-colors"
                >
                  Go to Partner Dashboard
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            </div>

            {/* Form */}
            <div className="rounded-2xl border border-gray-100 bg-white p-8 shadow-lg">
              {step === "register" && (
                <form onSubmit={handleRegister} className="space-y-5">
                  <div className="text-center mb-2">
                    <h3 className="text-xl font-bold text-text-dark">
                      Create Partner Account
                    </h3>
                    <p className="text-sm text-text-muted mt-1">
                      Step 1 of 2 — Registration
                    </p>
                  </div>
                  {/* Contact Person */}
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="contact_person_name"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Your Full Name
                      </label>
                      <input
                        id="contact_person_name"
                        type="text"
                        required
                        value={formData.contact_person_name}
                        onChange={(e) =>
                          setFormData({ ...formData, contact_person_name: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="John Moyo"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="contact_person_role"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Your Role
                      </label>
                      <input
                        id="contact_person_role"
                        type="text"
                        value={formData.contact_person_role}
                        onChange={(e) =>
                          setFormData({ ...formData, contact_person_role: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="Operations Manager"
                      />
                    </div>
                  </div>

                  {/* Company Info */}
                  <div>
                    <label
                      htmlFor="name"
                      className="block text-sm font-medium text-text-dark mb-1.5"
                    >
                      Company Name
                    </label>
                    <input
                      id="name"
                      type="text"
                      required
                      value={formData.name}
                      onChange={(e) =>
                        setFormData({ ...formData, name: e.target.value })
                      }
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                      placeholder="Acme Corp"
                    />
                  </div>
                  <div>
                    <label
                      htmlFor="description"
                      className="block text-sm font-medium text-text-dark mb-1.5"
                    >
                      What does your business do?
                    </label>
                    <textarea
                      id="description"
                      rows={2}
                      value={formData.description}
                      onChange={(e) =>
                        setFormData({ ...formData, description: e.target.value })
                      }
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition resize-none"
                      placeholder="E-commerce store selling electronics..."
                    />
                  </div>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="address"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Business Address
                      </label>
                      <input
                        id="address"
                        type="text"
                        value={formData.address}
                        onChange={(e) =>
                          setFormData({ ...formData, address: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="12 Samora Machel Ave"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="city"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        City
                      </label>
                      <input
                        id="city"
                        type="text"
                        value={formData.city}
                        onChange={(e) =>
                          setFormData({ ...formData, city: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="Harare"
                      />
                    </div>
                  </div>

                  {/* Contact Details */}
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="email"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Business Email
                      </label>
                      <input
                        id="email"
                        type="email"
                        required
                        value={formData.email}
                        onChange={(e) =>
                          setFormData({ ...formData, email: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="admin@company.co.zw"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="phone"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Phone Number
                      </label>
                      <input
                        id="phone"
                        type="tel"
                        required
                        value={formData.phone}
                        onChange={(e) =>
                          setFormData({ ...formData, phone: e.target.value })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="+263 77 123 4567"
                      />
                    </div>
                  </div>
                  <div>
                    <label
                      htmlFor="password"
                      className="block text-sm font-medium text-text-dark mb-1.5"
                    >
                      Password
                    </label>
                    <div className="relative">
                      <input
                        id="password"
                        type={showPassword ? "text" : "password"}
                        required
                        minLength={8}
                        value={formData.password}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            password: e.target.value,
                          })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 pr-11 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="Min. 8 characters"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      >
                        {showPassword ? (
                          <EyeOff className="h-4 w-4" />
                        ) : (
                          <Eye className="h-4 w-4" />
                        )}
                      </button>
                    </div>
                  </div>

                  {error && (
                    <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
                      {error}
                    </p>
                  )}

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full rounded-xl bg-accent py-3.5 text-sm font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Registering...
                      </>
                    ) : (
                      <>
                        Continue to Payment
                        <ArrowRight className="h-4 w-4" />
                      </>
                    )}
                  </button>

                  <p className="text-xs text-center text-text-muted">
                    By registering, you agree to our{" "}
                    <Link href="/terms" className="underline hover:text-primary">
                      Terms of Service
                    </Link>
                  </p>
                </form>
              )}

              {step === "payment" && (
                <form onSubmit={handlePaySetup} className="space-y-5">
                  <div className="text-center mb-2">
                    <h3 className="text-xl font-bold text-text-dark">
                      Pay Setup Fee
                    </h3>
                    <p className="text-sm text-text-muted mt-1">
                      Step 2 of 2 — $15.00 one-time fee
                    </p>
                  </div>

                  <div className="rounded-xl bg-primary-light p-4 text-center">
                    <p className="text-3xl font-extrabold text-primary-dark">
                      $15.00
                    </p>
                    <p className="text-sm text-text-muted mt-1">
                      One-time setup fee
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-text-dark mb-3">
                      Payment Method
                    </label>
                    <div className="grid grid-cols-3 gap-3">
                      {[
                        { value: "ecocash", label: "EcoCash" },
                        { value: "onemoney", label: "OneMoney" },
                        { value: "card", label: "Card" },
                      ].map((method) => (
                        <button
                          key={method.value}
                          type="button"
                          onClick={() => setPaymentMethod(method.value)}
                          className={`rounded-xl border-2 px-4 py-3 text-sm font-medium transition-all ${
                            paymentMethod === method.value
                              ? "border-primary bg-primary-light text-primary"
                              : "border-gray-200 text-text-dark hover:border-gray-300"
                          }`}
                        >
                          {method.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  {(paymentMethod === "ecocash" ||
                    paymentMethod === "onemoney") && (
                    <div>
                      <label
                        htmlFor="payPhone"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Phone Number
                      </label>
                      <input
                        id="payPhone"
                        type="tel"
                        required
                        value={paymentPhone}
                        onChange={(e) => setPaymentPhone(e.target.value)}
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="0771234567"
                      />
                    </div>
                  )}

                  {error && (
                    <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
                      {error}
                    </p>
                  )}

                  <button
                    type="submit"
                    disabled={loading || !paymentMethod}
                    className="w-full rounded-xl bg-accent py-3.5 text-sm font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>Pay $15.00</>
                    )}
                  </button>
                </form>
              )}

              {step === "polling" && (
                <div className="text-center py-8 space-y-4">
                  <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto" />
                  <h3 className="text-xl font-bold text-text-dark">
                    Waiting for Payment
                  </h3>
                  <p className="text-sm text-text-muted max-w-sm mx-auto">
                    {redirectUrl ? (
                      <>
                        Complete your card payment in the new window.{" "}
                        <a
                          href={redirectUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-primary underline"
                        >
                          Open payment page
                        </a>
                      </>
                    ) : (
                      "Please confirm the payment on your phone. This page will update automatically."
                    )}
                  </p>
                  <button
                    onClick={() => {
                      if (pollInterval) clearInterval(pollInterval);
                      setPollInterval(null);
                      setStep("payment");
                    }}
                    className="text-sm text-text-muted hover:text-text-dark underline"
                  >
                    Try a different payment method
                  </button>
                </div>
              )}

              {step === "done" && (
                <div className="text-center py-8 space-y-4">
                  <div className="mx-auto mb-2 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                    <CheckCircle className="h-8 w-8 text-green-600" />
                  </div>
                  <h3 className="text-xl font-bold text-text-dark">
                    You&apos;re All Set!
                  </h3>
                  <p className="text-sm text-text-muted max-w-sm mx-auto">
                    Your partner account is now active. Log in to your
                    dashboard to view your API key and create device login
                    credentials.
                  </p>
                  <Link
                    href="/partner/dashboard"
                    className="inline-flex items-center gap-2 rounded-xl bg-primary px-8 py-3 text-sm font-semibold text-white transition-all hover:bg-primary-dark hover:shadow-lg"
                  >
                    Go to Dashboard
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
