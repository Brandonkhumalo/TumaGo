"use client";

import { useState, type FormEvent } from "react";
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
} from "lucide-react";

const benefits = [
  {
    icon: Code,
    title: "API Integration",
    description:
      "Access our RESTful API to create deliveries, track packages, and receive real-time webhooks — all programmatically. Integrate TumaGo directly into your existing systems.",
  },
  {
    icon: HeadphonesIcon,
    title: "Dedicated Support",
    description:
      "Get a dedicated account manager who understands your business. Priority support, custom onboarding, and regular performance reviews to optimize your delivery operations.",
  },
  {
    icon: TrendingUp,
    title: "Volume Pricing",
    description:
      "Benefit from competitive tiered pricing as your delivery volume grows. The more you ship, the more you save. No hidden fees, transparent billing every month.",
  },
  {
    icon: MapPin,
    title: "Real-Time Tracking",
    description:
      "Give your customers full visibility with live GPS tracking on every delivery. Reduce support inquiries with automated status notifications via SMS or webhook.",
  },
  {
    icon: BarChart3,
    title: "Analytics Dashboard",
    description:
      "Access detailed analytics on delivery performance, driver ratings, delivery times, and costs. Make data-driven decisions to optimize your logistics.",
  },
  {
    icon: Shield,
    title: "Insured Deliveries",
    description:
      "Every partner delivery is covered by our insurance policy. Ship with confidence knowing your packages and your customers are protected.",
  },
];

const integrationSteps = [
  {
    number: "01",
    title: "Sign Up",
    description:
      "Create a business partner account through our partner portal. Tell us about your company, delivery volume, and integration needs.",
  },
  {
    number: "02",
    title: "Get Your API Key",
    description:
      "Receive your API credentials and access our comprehensive documentation. Our sandbox environment lets you test everything before going live.",
  },
  {
    number: "03",
    title: "Integrate",
    description:
      "Use our RESTful API, webhooks, and SDKs to connect TumaGo with your platform. Our engineering team provides hands-on integration support.",
  },
  {
    number: "04",
    title: "Launch",
    description:
      "Go live with confidence. We handle the fleet, the tracking, and the payments. You focus on your business while we handle the last mile.",
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

export default function PartnerPage() {
  const [formData, setFormData] = useState({
    name: "",
    company: "",
    email: "",
    phone: "",
    volume: "",
    message: "",
  });
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    // Client-side only for now — will connect to backend later
    setSubmitted(true);
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
            Integrate reliable, trackable last-mile delivery into your
            business. Our API-first platform gives you full control over
            your delivery operations — from automated dispatch to real-time
            customer updates.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:justify-center">
            <a
              href="#contact-form"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-8 py-4 text-base font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl hover:-translate-y-0.5"
            >
              Become a Partner
              <ArrowRight className="h-5 w-5" />
            </a>
            <a
              href="#how-it-works"
              className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-white/30 px-8 py-4 text-base font-semibold text-white transition-all hover:bg-white/10"
            >
              See How It Works
            </a>
          </div>
        </div>
      </section>

      {/* Industries Section */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Industries We Serve
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Delivery Solutions for Every Business
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              From e-commerce to healthcare, TumaGo powers delivery for
              businesses of all sizes across Zimbabwe.
            </p>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {industries.map((industry) => (
              <div
                key={industry.name}
                className="group flex items-start gap-4 rounded-2xl border border-gray-100 p-6 transition-all hover:shadow-lg hover:border-primary/20 hover:-translate-y-1"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <industry.icon className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="font-bold text-text-dark">
                    {industry.name}
                  </h3>
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
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Partner Benefits
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Why Businesses Choose TumaGo
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              Everything you need to offer world-class delivery to your
              customers, without building the infrastructure yourself.
            </p>
          </div>
          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {benefits.map((benefit) => (
              <div
                key={benefit.title}
                className="group rounded-2xl bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:-translate-y-1"
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

      {/* How It Works Section */}
      <section id="how-it-works" className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Integration Process
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Get Started in Four Steps
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              From signup to launch, our team supports you every step of
              the way. Most partners are live within a week.
            </p>
          </div>
          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">
            {integrationSteps.map((step) => (
              <div
                key={step.number}
                className="relative rounded-2xl border border-gray-100 bg-white p-8 transition-all hover:shadow-lg hover:border-primary/20"
              >
                <span className="text-5xl font-extrabold text-primary/10">
                  {step.number}
                </span>
                <h3 className="mt-4 text-lg font-bold text-text-dark">
                  {step.title}
                </h3>
                <p className="mt-3 text-sm text-text-muted leading-relaxed">
                  {step.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing Section — Balance-Based */}
      <section className="py-20 lg:py-24 bg-gradient-to-r from-primary-dark to-primary text-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          {/* Header */}
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold sm:text-4xl">
              Simple, Transparent Pricing
            </h2>
            <p className="mt-4 text-lg text-blue-100/80 max-w-2xl mx-auto">
              No hidden fees. Deposit funds, request deliveries, and only pay
              for what you use. Every dollar is accounted for.
            </p>
          </div>

          {/* Setup Fee Card */}
          <div className="mx-auto max-w-md mb-16">
            <div className="rounded-2xl bg-white p-8 text-center shadow-xl">
              <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                <DollarSign className="h-7 w-7 text-primary" />
              </div>
              <p className="text-4xl font-extrabold text-primary-dark">
                $15
              </p>
              <p className="mt-1 text-sm font-semibold text-text-dark uppercase tracking-wider">
                One-Time Setup Fee
              </p>
              <p className="mt-3 text-sm text-text-muted leading-relaxed">
                Get your API credentials and start integrating. Charged from
                your first deposit — no upfront payment needed.
              </p>
              <a
                href="#contact-form"
                className="mt-6 inline-block rounded-xl bg-accent px-8 py-3 text-sm font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl"
              >
                Get Started
              </a>
            </div>
          </div>

          {/* How Billing Works — 3 Steps */}
          <div className="mb-16">
            <h3 className="text-center text-xl font-bold mb-10">
              How Billing Works
            </h3>
            <div className="grid gap-8 md:grid-cols-3">
              {[
                {
                  step: "01",
                  icon: Wallet,
                  title: "Deposit Funds",
                  description:
                    "Add money to your TumaGo partner account via bank transfer or mobile money. Your balance is available instantly.",
                },
                {
                  step: "02",
                  icon: Package,
                  title: "Request Deliveries",
                  description:
                    "Each delivery fare is automatically deducted from your balance. No invoices, no delays — it just works.",
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
                  className="rounded-2xl bg-white/10 backdrop-blur p-8 text-center"
                >
                  <span className="text-5xl font-extrabold text-white/20">
                    {item.step}
                  </span>
                  <div className="mx-auto mt-4 mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-white/20">
                    <item.icon className="h-6 w-6 text-white" />
                  </div>
                  <h4 className="text-lg font-bold">{item.title}</h4>
                  <p className="mt-3 text-sm text-blue-100/80 leading-relaxed">
                    {item.description}
                  </p>
                </div>
              ))}
            </div>
          </div>

          {/* Commission Structure */}
          <div>
            <h3 className="text-center text-xl font-bold mb-10">
              Commission Structure
            </h3>
            <div className="grid gap-8 md:grid-cols-2 max-w-3xl mx-auto">
              {/* Platform Fee */}
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

              {/* Driver Share */}
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

            {/* Example breakdown */}
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

      {/* Contact Form Section */}
      <section id="contact-form" className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-2 lg:items-start">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
                Get In Touch
              </p>
              <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
                Let&apos;s Build Something Together
              </h2>
              <p className="mt-4 text-text-muted leading-relaxed">
                Tell us about your business and delivery needs. Our
                partnerships team will get back to you within 24 hours with
                a custom proposal.
              </p>
              <div className="mt-8 space-y-6">
                {[
                  {
                    title: "Quick Integration",
                    desc: "Most partners go live within 5 business days",
                  },
                  {
                    title: "Pay-As-You-Go",
                    desc: "Deposit funds and only pay for deliveries you use",
                  },
                  {
                    title: "Dedicated Support",
                    desc: "A real person who knows your account, not a ticket queue",
                  },
                ].map((item) => (
                  <div
                    key={item.title}
                    className="flex items-start gap-4"
                  >
                    <CheckCircle className="h-6 w-6 shrink-0 text-primary mt-0.5" />
                    <div>
                      <p className="font-semibold text-text-dark">
                        {item.title}
                      </p>
                      <p className="text-sm text-text-muted">
                        {item.desc}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Form */}
            <div className="rounded-2xl border border-gray-100 bg-white p-8 shadow-lg">
              {submitted ? (
                <div className="text-center py-12">
                  <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                    <CheckCircle className="h-8 w-8 text-green-600" />
                  </div>
                  <h3 className="text-xl font-bold text-text-dark">
                    Thank You!
                  </h3>
                  <p className="mt-2 text-text-muted">
                    We have received your inquiry. Our partnerships team
                    will reach out within 24 hours.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-5">
                  <div className="grid gap-5 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="name"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Full Name
                      </label>
                      <input
                        id="name"
                        type="text"
                        required
                        value={formData.name}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            name: e.target.value,
                          })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="John Doe"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="company"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Company Name
                      </label>
                      <input
                        id="company"
                        type="text"
                        required
                        value={formData.company}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            company: e.target.value,
                          })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="Acme Corp"
                      />
                    </div>
                  </div>
                  <div className="grid gap-5 sm:grid-cols-2">
                    <div>
                      <label
                        htmlFor="email"
                        className="block text-sm font-medium text-text-dark mb-1.5"
                      >
                        Work Email
                      </label>
                      <input
                        id="email"
                        type="email"
                        required
                        value={formData.email}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            email: e.target.value,
                          })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="john@company.co.zw"
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
                        value={formData.phone}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            phone: e.target.value,
                          })
                        }
                        className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                        placeholder="+263 77 123 4567"
                      />
                    </div>
                  </div>
                  <div>
                    <label
                      htmlFor="volume"
                      className="block text-sm font-medium text-text-dark mb-1.5"
                    >
                      Estimated Monthly Deliveries
                    </label>
                    <select
                      id="volume"
                      value={formData.volume}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          volume: e.target.value,
                        })
                      }
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition"
                    >
                      <option value="">Select range</option>
                      <option value="1-50">1 - 50</option>
                      <option value="51-200">51 - 200</option>
                      <option value="201-500">201 - 500</option>
                      <option value="501-1000">501 - 1,000</option>
                      <option value="1000+">1,000+</option>
                    </select>
                  </div>
                  <div>
                    <label
                      htmlFor="message"
                      className="block text-sm font-medium text-text-dark mb-1.5"
                    >
                      Tell Us About Your Needs
                    </label>
                    <textarea
                      id="message"
                      rows={4}
                      value={formData.message}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          message: e.target.value,
                        })
                      }
                      className="w-full rounded-xl border border-gray-200 px-4 py-3 text-sm text-text-dark placeholder:text-gray-400 focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none transition resize-none"
                      placeholder="What kind of deliveries does your business need? Any specific integration requirements?"
                    />
                  </div>
                  <button
                    type="submit"
                    className="w-full rounded-xl bg-accent py-3.5 text-sm font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl"
                  >
                    Submit Partnership Inquiry
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
