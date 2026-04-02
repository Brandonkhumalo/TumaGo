import type { Metadata } from "next";
import Image from "next/image";
import {
  Shield,
  Zap,
  Heart,
  Lightbulb,
  MapPin,
  Package,
  TrendingUp,
  Users,
  Target,
  Globe,
} from "lucide-react";

export const metadata: Metadata = {
  title: "About Us — TumaGo",
  description:
    "Learn about TumaGo's mission to revolutionize last-mile delivery in Zimbabwe. Our story, values, and vision for the future.",
};

const values = [
  {
    icon: Shield,
    title: "Reliability",
    description:
      "Every delivery we handle is a promise kept. We invest in verified drivers, real-time tracking, and secure payments so you never have to worry about your package reaching its destination.",
  },
  {
    icon: Zap,
    title: "Speed",
    description:
      "Time matters. Our geo-based matching algorithm connects you with the nearest available driver in seconds, and our optimized routing ensures the fastest possible delivery time across the city.",
  },
  {
    icon: Heart,
    title: "Trust",
    description:
      "We build trust through transparency. Every driver is verified, every delivery is tracked, and every transaction is secured. Our rating system ensures accountability at every step.",
  },
  {
    icon: Lightbulb,
    title: "Innovation",
    description:
      "We leverage cutting-edge technology — WebSocket live tracking, intelligent matching algorithms, and mobile-first design — to solve real delivery challenges in Zimbabwe's growing economy.",
  },
];

const milestones = [
  {
    year: "May 2025",
    title: "The Idea",
    description:
      "Identified the gap in Zimbabwe's last-mile delivery market. Small businesses and individuals needed a reliable, affordable way to send packages across the city without owning a vehicle. The concept for TumaGo was born.",
  },
  {
    year: "2025",
    title: "Development Begins",
    description:
      "Built the platform from the ground up with a hybrid Go and Django backend, Android apps for senders and drivers, and real-time WebSocket tracking for live GPS updates.",
  },
  {
    year: "2025",
    title: "Harare Launch",
    description:
      "Launched in Harare with an initial fleet of verified drivers. Integrated EcoCash and OneMoney for seamless mobile money payments — the payment methods Zimbabweans already use daily.",
  },
  {
    year: "2026",
    title: "Growing the Network",
    description:
      "Currently operating in Harare with a growing fleet of drivers. Drivers from across Zimbabwe are free to register and join the platform as we prepare for nationwide expansion.",
  },
  {
    year: "2026",
    title: "Scaling Up",
    description:
      "Building WhatsApp integration for conversational delivery requests, expanding to more cities, and developing advanced analytics for business partners. The journey continues.",
  },
];

const teamStats = [
  { icon: Users, value: "20+", label: "Team Members" },
  { icon: MapPin, value: "3", label: "Cities" },
  { icon: Package, value: "10,000+", label: "Deliveries" },
  { icon: TrendingUp, value: "500+", label: "Drivers" },
];

export default function AboutPage() {
  return (
    <main>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-dark to-primary pt-28 pb-20 lg:pt-36 lg:pb-24">
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-white/5" />
          <div className="absolute bottom-0 left-0 h-60 w-60 rounded-full bg-white/5" />
        </div>
        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-wider text-blue-200 mb-4">
            Our Story
          </p>
          <h1 className="text-4xl font-extrabold text-white sm:text-5xl lg:text-6xl">
            About TumaGo
          </h1>
          <p className="mt-6 text-lg text-blue-100/90 max-w-2xl mx-auto leading-relaxed">
            We are on a mission to make last-mile package delivery fast,
            reliable, and accessible for every person and business in
            Zimbabwe.
          </p>
        </div>
      </section>

      {/* Mission Section */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-2 lg:items-center">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
                Our Mission
              </p>
              <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
                Revolutionizing Last-Mile Delivery in Zimbabwe
              </h2>
              <p className="mt-6 text-text-muted leading-relaxed">
                Zimbabwe&apos;s economy is growing, and with that growth
                comes a surge in demand for reliable delivery services.
                Small businesses need to get products to customers.
                Families need to send packages to loved ones across the
                city. Yet traditional courier services are expensive, slow,
                and opaque.
              </p>
              <p className="mt-4 text-text-muted leading-relaxed">
                TumaGo was born from a simple question: what if anyone
                could request a delivery as easily as ordering a ride? We
                built a platform that connects senders with a network of
                verified drivers, enables real-time GPS tracking, and
                supports the payment methods Zimbabweans already use —
                EcoCash, OneMoney, and card.
              </p>
              <p className="mt-4 text-text-muted leading-relaxed">
                &quot;Tuma&quot; means &quot;send&quot; in Shona. TumaGo
                is your go-to platform for sending anything, anywhere in
                the city. Fast. Reliable. Affordable.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 overflow-hidden rounded-2xl">
                <Image
                  src="/headoffice1.png"
                  alt="TumaGo headquarters in Msasa, Harare"
                  width={600}
                  height={300}
                  className="w-full h-48 object-cover"
                />
              </div>
              <div className="overflow-hidden rounded-2xl">
                <Image
                  src="/depo.png"
                  alt="TumaGo delivery fleet at the depot"
                  width={300}
                  height={200}
                  className="w-full h-40 object-cover"
                />
              </div>
              <div className="overflow-hidden rounded-2xl">
                <Image
                  src="/parcel_handover.png"
                  alt="TumaGo driver handing over a parcel"
                  width={300}
                  height={200}
                  className="w-full h-40 object-cover"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Values Section */}
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Our Values
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              What We Stand For
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              These four principles guide every decision we make, every
              feature we build, and every interaction we have.
            </p>
          </div>
          <div className="grid gap-8 sm:grid-cols-2">
            {values.map((value) => (
              <div
                key={value.title}
                className="group rounded-2xl bg-white p-8 shadow-sm transition-all hover:shadow-lg hover:-translate-y-1"
              >
                <div className="mb-5 inline-flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <value.icon className="h-7 w-7" />
                </div>
                <h3 className="text-xl font-bold text-text-dark mb-3">
                  {value.title}
                </h3>
                <p className="text-text-muted leading-relaxed">
                  {value.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Our Journey Timeline */}
      <section className="py-20 lg:py-28 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Our Journey
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              From Idea to Impact
            </h2>
            <p className="mt-4 text-lg text-text-muted">
              Every great company starts with a problem worth solving.
              Here is how TumaGo went from a concept to a platform
              serving thousands.
            </p>
          </div>
          <div className="relative">
            {/* Timeline Line */}
            <div className="absolute left-8 top-0 bottom-0 w-0.5 bg-primary/20 hidden md:block md:left-1/2 md:-translate-x-px" />

            <div className="space-y-12">
              {milestones.map((milestone, index) => (
                <div
                  key={milestone.title}
                  className={`relative flex flex-col md:flex-row md:items-center gap-8 ${
                    index % 2 === 0 ? "md:flex-row" : "md:flex-row-reverse"
                  }`}
                >
                  {/* Content */}
                  <div className="flex-1 md:text-right">
                    <div
                      className={`rounded-2xl bg-primary-light p-6 ${
                        index % 2 === 0
                          ? "md:text-right"
                          : "md:text-left"
                      }`}
                    >
                      <span className="inline-block rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary mb-2">
                        {milestone.year}
                      </span>
                      <h3 className="text-lg font-bold text-text-dark">
                        {milestone.title}
                      </h3>
                      <p className="mt-2 text-sm text-text-muted leading-relaxed">
                        {milestone.description}
                      </p>
                    </div>
                  </div>

                  {/* Dot */}
                  <div className="absolute left-8 md:left-1/2 md:-translate-x-1/2 h-4 w-4 rounded-full bg-primary border-4 border-white shadow hidden md:block" />

                  {/* Spacer */}
                  <div className="flex-1" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Zimbabwe Focus Section */}
      <section className="py-20 lg:py-24 bg-gradient-to-r from-primary-dark to-primary text-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-2 lg:items-center">
            <div>
              <div className="flex items-center gap-3 mb-6">
                <Globe className="h-6 w-6" />
                <p className="text-sm font-semibold uppercase tracking-wider text-blue-200">
                  Built for Zimbabwe
                </p>
              </div>
              <h2 className="text-3xl font-bold sm:text-4xl">
                Local Problem, Local Solution
              </h2>
              <p className="mt-6 text-blue-100/90 leading-relaxed">
                TumaGo is designed specifically for the Zimbabwean market.
                We understand the payment landscape — that is why we
                support EcoCash and OneMoney alongside card payments. We
                understand the cities — our drivers know the streets of
                Harare inside and out.
              </p>
              <p className="mt-4 text-blue-100/90 leading-relaxed">
                While our delivery operations are currently focused in
                Harare, drivers from anywhere in Zimbabwe are welcome to
                register on the platform. As we grow, we will be expanding
                to Bulawayo, Mutare, and beyond — and having a nationwide
                network of drivers ready to go is how we will make that
                happen.
              </p>
              <p className="mt-4 text-blue-100/90 leading-relaxed">
                We are not just building a delivery app. We are creating
                economic opportunity for drivers, enabling small businesses
                to reach their customers, and making urban life a little
                more convenient for everyone.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 overflow-hidden rounded-2xl">
                <Image
                  src="/headoffice2.png"
                  alt="TumaGo team and fleet at headquarters"
                  width={600}
                  height={250}
                  className="w-full h-48 object-cover"
                />
              </div>
              <div className="overflow-hidden rounded-2xl">
                <Image
                  src="/headoffice3.png"
                  alt="TumaGo fleet of delivery bikes"
                  width={300}
                  height={200}
                  className="w-full h-40 object-cover"
                />
              </div>
              <div className="rounded-2xl bg-white/10 p-6 backdrop-blur flex flex-col justify-center">
                <Users className="h-8 w-8 text-blue-200 mb-3" />
                <h3 className="font-bold text-lg">Drivers Nationwide</h3>
                <p className="text-sm text-blue-100/80 mt-1">
                  Drivers from anywhere in Zimbabwe can register now and be
                  ready when we expand to their city
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Parent Company */}
      <section className="py-16 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
            A Product Of
          </p>
          <h2 className="text-2xl font-bold text-text-dark sm:text-3xl">
            <a
              href="https://tishanyq.co.zw"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-primary transition-colors"
            >
              Tishanyq Digital Pvt Ltd
            </a>
          </h2>
          <p className="mt-3 text-text-muted max-w-lg mx-auto">
            TumaGo is proudly built and operated by Tishanyq Digital Pvt
            Ltd, a Zimbabwean technology company focused on building
            digital solutions that solve real problems for real people.
          </p>
          <a
            href="https://tishanyq.co.zw"
            target="_blank"
            rel="noopener noreferrer"
            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark transition-colors"
          >
            Visit tishanyq.co.zw
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="h-4 w-4">
              <path fillRule="evenodd" d="M5.22 14.78a.75.75 0 001.06 0l7.22-7.22v5.69a.75.75 0 001.5 0v-7.5a.75.75 0 00-.75-.75h-7.5a.75.75 0 000 1.5h5.69l-7.22 7.22a.75.75 0 000 1.06z" clipRule="evenodd" />
            </svg>
          </a>
        </div>
      </section>

      {/* Join Us CTA */}
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
            Join the TumaGo Journey
          </h2>
          <p className="mt-4 text-lg text-text-muted max-w-lg mx-auto">
            Whether you want to send a package, drive and earn, or partner
            your business with us — there is a place for you in the TumaGo
            community.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:justify-center">
            <a
              href="#"
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-accent px-8 py-4 text-base font-semibold text-white shadow-lg shadow-accent/25 transition-all hover:bg-orange-600 hover:shadow-xl hover:-translate-y-0.5"
            >
              Download the App
            </a>
            <a
              href="/partner"
              className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-primary px-8 py-4 text-base font-semibold text-primary transition-all hover:bg-primary hover:text-white"
            >
              Partner With Us
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
