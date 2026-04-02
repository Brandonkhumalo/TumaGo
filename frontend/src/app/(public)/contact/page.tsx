"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Mail,
  Phone,
  MapPin,
  Clock,
  MessageCircle,
  HelpCircle,
  Headphones,
  ChevronDown,
} from "lucide-react";

const contactMethods = [
  {
    icon: Mail,
    title: "Email Us",
    description: "For general inquiries and support",
    value: "info@tumago.co.zw",
    href: "mailto:info@tumago.co.zw",
  },
  {
    icon: Phone,
    title: "Call Us",
    description: "Monday - Friday, 8:30 AM - 4:30 PM",
    value: "078 160 3382 / 024 270 7269",
    href: "tel:+263781603382",
  },
  {
    icon: MapPin,
    title: "Headquarters",
    description: "Visit our office in Harare",
    value: "7 Martin Drive, Msasa, Harare, Zimbabwe",
    href: "#map",
  },
  {
    icon: Clock,
    title: "Business Hours",
    description: "Office hours",
    value: "Mon - Fri: 8:30 AM - 4:30 PM",
    href: undefined,
  },
];

const supportTopics = [
  {
    icon: MessageCircle,
    title: "General Inquiry",
    description:
      "Questions about TumaGo services, pricing, or availability in your area.",
  },
  {
    icon: Headphones,
    title: "Customer Support",
    description:
      "Issues with a delivery, payment, or your account. We are here to help.",
  },
  {
    icon: HelpCircle,
    title: "Driver Support",
    description:
      "Questions about becoming a driver, payouts, or the driver app.",
  },
];

export default function ContactPage() {
  return (
    <main>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-primary-dark to-primary pt-28 pb-20 lg:pt-36 lg:pb-24">
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 -right-40 h-80 w-80 rounded-full bg-white/5" />
          <div className="absolute bottom-10 left-10 h-40 w-40 rounded-full bg-white/5" />
        </div>
        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-wider text-blue-200 mb-4">
            Contact Us
          </p>
          <h1 className="text-4xl font-extrabold text-white sm:text-5xl lg:text-6xl">
            Get in Touch
          </h1>
          <p className="mt-6 text-lg text-blue-100/90 max-w-2xl mx-auto leading-relaxed">
            Have a question, need support, or want to partner with us? We
            would love to hear from you. Our team typically responds within
            a few hours during business hours.
          </p>
        </div>
      </section>

      {/* Photo Banner */}
      <section className="bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 -mt-10 relative z-10">
          <div className="grid grid-cols-3 gap-3 overflow-hidden rounded-2xl shadow-xl">
            <Image src="/parcel_handover.png" alt="Package handover" width={400} height={250} className="w-full h-32 sm:h-48 lg:h-56 object-cover" />
            <Image src="/depo.png" alt="TumaGo depot" width={400} height={250} className="w-full h-32 sm:h-48 lg:h-56 object-cover" />
            <Image src="/parce_handover2.png" alt="Driver delivering package" width={400} height={250} className="w-full h-32 sm:h-48 lg:h-56 object-cover" />
          </div>
        </div>
      </section>

      {/* Contact Methods */}
      <section className="pt-12 pb-16 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {contactMethods.map((method) => (
              <div
                key={method.title}
                className="group rounded-2xl border border-gray-100 p-6 text-center transition-all hover:shadow-lg hover:border-primary/20 hover:-translate-y-1"
              >
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-white">
                  <method.icon className="h-7 w-7" />
                </div>
                <h3 className="font-bold text-text-dark">
                  {method.title}
                </h3>
                <p className="mt-1 text-xs text-text-muted">
                  {method.description}
                </p>
                {method.href ? (
                  <a
                    href={method.href}
                    className="mt-3 block text-sm font-medium text-primary hover:text-primary-dark transition-colors"
                  >
                    {method.value}
                  </a>
                ) : (
                  <p className="mt-3 text-sm font-medium text-text-dark">
                    {method.value}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Support + Map Section */}
      <section className="py-20 lg:py-28 bg-primary-light">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-8 sm:grid-cols-2">
              {/* Support Topics */}
              <div>
                <h3 className="text-lg font-bold text-text-dark mb-4">
                  How Can We Help?
                </h3>
                <div className="space-y-4">
                  {supportTopics.map((topic) => (
                    <div
                      key={topic.title}
                      className="flex items-start gap-4 rounded-xl bg-white p-5 shadow-sm"
                    >
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <topic.icon className="h-5 w-5" />
                      </div>
                      <div>
                        <p className="font-semibold text-text-dark text-sm">
                          {topic.title}
                        </p>
                        <p className="mt-1 text-xs text-text-muted leading-relaxed">
                          {topic.description}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 24/7 WhatsApp Support */}
              <div>
                <h3 className="text-lg font-bold text-text-dark mb-4">
                  24/7 WhatsApp Support
                </h3>
                <a
                  href="https://wa.me/263781603382"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-4 rounded-2xl bg-green-50 border border-green-200 p-5 transition-all hover:shadow-md hover:-translate-y-0.5"
                >
                  <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-green-500 text-white">
                    <svg viewBox="0 0 24 24" fill="currentColor" className="h-7 w-7">
                      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
                    </svg>
                  </div>
                  <div>
                    <p className="font-bold text-green-800">
                      Chat with us on WhatsApp
                    </p>
                    <p className="text-sm text-green-700 mt-0.5">
                      +263 78 160 3382 — available 24 hours, 7 days a week
                    </p>
                  </div>
                </a>
              </div>

              {/* Social Links */}
              <div>
                <h3 className="text-lg font-bold text-text-dark mb-4">
                  Follow Us
                </h3>
                <div className="flex gap-3">
                  {[
                    {
                      name: "WhatsApp",
                      href: "https://wa.me/263781603382",
                      svg: (
                        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
                      ),
                    },
                    {
                      name: "Twitter",
                      href: "https://twitter.com/tumago_zw",
                      svg: (
                        <path d="M23.953 4.57a10 10 0 01-2.825.775 4.958 4.958 0 002.163-2.723c-.951.555-2.005.959-3.127 1.184a4.92 4.92 0 00-8.384 4.482C7.69 8.095 4.067 6.13 1.64 3.162a4.822 4.822 0 00-.666 2.475c0 1.71.87 3.213 2.188 4.096a4.904 4.904 0 01-2.228-.616v.06a4.923 4.923 0 003.946 4.827 4.996 4.996 0 01-2.212.085 4.936 4.936 0 004.604 3.417 9.867 9.867 0 01-6.102 2.105c-.39 0-.779-.023-1.17-.067a13.995 13.995 0 007.557 2.209c9.053 0 13.998-7.496 13.998-13.985 0-.21 0-.42-.015-.63A9.935 9.935 0 0024 4.59z" />
                      ),
                    },
                    {
                      name: "Facebook",
                      href: "https://facebook.com/tumago.zw",
                      svg: (
                        <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                      ),
                    },
                    {
                      name: "Instagram",
                      href: "https://instagram.com/tumago.zw",
                      svg: (
                        <path d="M12 0C8.74 0 8.333.015 7.053.072 5.775.132 4.905.333 4.14.63c-.789.306-1.459.717-2.126 1.384S.935 3.35.63 4.14C.333 4.905.131 5.775.072 7.053.012 8.333 0 8.74 0 12s.015 3.667.072 4.947c.06 1.277.261 2.148.558 2.913.306.788.717 1.459 1.384 2.126.667.666 1.336 1.079 2.126 1.384.766.296 1.636.499 2.913.558C8.333 23.988 8.74 24 12 24s3.667-.015 4.947-.072c1.277-.06 2.148-.262 2.913-.558.788-.306 1.459-.718 2.126-1.384.666-.667 1.079-1.335 1.384-2.126.296-.765.499-1.636.558-2.913.06-1.28.072-1.687.072-4.947s-.015-3.667-.072-4.947c-.06-1.277-.262-2.149-.558-2.913-.306-.789-.718-1.459-1.384-2.126C21.319 1.347 20.651.935 19.86.63c-.765-.297-1.636-.499-2.913-.558C15.667.012 15.26 0 12 0zm0 2.16c3.203 0 3.585.016 4.85.071 1.17.055 1.805.249 2.227.415.562.217.96.477 1.382.896.419.42.679.819.896 1.381.164.422.36 1.057.413 2.227.057 1.266.07 1.646.07 4.85s-.015 3.585-.074 4.85c-.061 1.17-.256 1.805-.421 2.227-.224.562-.479.96-.899 1.382-.419.419-.824.679-1.38.896-.42.164-1.065.36-2.235.413-1.274.057-1.649.07-4.859.07-3.211 0-3.586-.015-4.859-.074-1.171-.061-1.816-.256-2.236-.421-.569-.224-.96-.479-1.379-.899-.421-.419-.69-.824-.9-1.38-.165-.42-.359-1.065-.42-2.235-.045-1.26-.061-1.649-.061-4.844 0-3.196.016-3.586.061-4.861.061-1.17.255-1.814.42-2.234.21-.57.479-.96.9-1.381.419-.419.81-.689 1.379-.898.42-.166 1.051-.361 2.221-.421 1.275-.045 1.65-.06 4.859-.06l.045.03zm0 3.678a6.162 6.162 0 100 12.324 6.162 6.162 0 100-12.324zM12 16c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4zm7.846-10.405a1.441 1.441 0 11-2.882 0 1.441 1.441 0 012.882 0z" />
                      ),
                    },
                  ].map((social) => (
                    <a
                      key={social.name}
                      href={social.href}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex h-12 w-12 items-center justify-center rounded-xl bg-white text-text-muted shadow-sm transition-all hover:text-primary hover:shadow-md hover:-translate-y-0.5"
                      aria-label={social.name}
                    >
                      <svg
                        viewBox="0 0 24 24"
                        fill="currentColor"
                        className="h-5 w-5"
                      >
                        {social.svg}
                      </svg>
                    </a>
                  ))}
                </div>
              </div>
          </div>
        </div>
      </section>

      {/* Find Us — Full-width Map */}
      <section id="map" className="py-16 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-8">
            <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
              Find Us
            </p>
            <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
              Our Location
            </h2>
          </div>
          <div className="overflow-hidden rounded-2xl shadow-lg">
            <iframe
              src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3798.5!2d31.1!3d-17.82!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2s7+Martin+Drive%2C+Msasa%2C+Harare%2C+Zimbabwe!5e0!3m2!1sen!2szw!4v1700000000000"
              width="100%"
              className="min-h-[300px] sm:min-h-[400px] lg:min-h-[500px]"
              style={{ border: 0 }}
              allowFullScreen
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              title="TumaGo Office Location"
            />
          </div>
          <div className="mt-4 flex flex-col sm:flex-row items-center justify-center gap-4 text-center">
            <div>
              <p className="font-semibold text-text-dark">7 Martin Drive, Msasa, Harare, Zimbabwe</p>
            </div>
            <a
              href="https://maps.google.com/?q=7+Martin+Drive+Msasa+Harare+Zimbabwe"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark transition-colors"
            >
              <MapPin className="h-4 w-4" />
              Open in Google Maps
            </a>
          </div>
        </div>
      </section>

      {/* FAQ Section */}
      <FAQSection />
    </main>
  );
}

const faqs = [
  {
    q: "What cities does TumaGo operate in?",
    a: "TumaGo currently operates in Harare, Zimbabwe. We are actively planning expansion to Bulawayo, Mutare, Gweru, and other major cities. Drivers from across Zimbabwe are welcome to register.",
  },
  {
    q: "What payment methods are accepted?",
    a: "We accept EcoCash, OneMoney, and card payments (Visa/Mastercard). All transactions are processed securely through our payment partner Paynow. Cash payments are also available — you pay the driver directly on delivery.",
  },
  {
    q: "How long does a typical delivery take?",
    a: "Most same-city deliveries in Harare are completed within 25-45 minutes depending on distance and traffic. You can track your package in real-time through the TumaGo app from pickup to dropoff.",
  },
  {
    q: "How do I become a TumaGo driver?",
    a: "Download the TumaGo Driver app from Google Play, sign up with your details, upload your driver's license and vehicle documents, and complete the verification process. Once approved, top up your delivery wallet and start accepting trips.",
  },
  {
    q: "What vehicle types are available for deliveries?",
    a: "We offer three vehicle options: Scooter (small packages, fastest and most affordable), Van (medium to large packages), and Truck (bulk or heavy items). Pricing varies by vehicle type and distance.",
  },
  {
    q: "Can businesses integrate with TumaGo?",
    a: "Yes! Our B2B Partner API allows e-commerce platforms, food delivery services, pharmacies, and other businesses to request deliveries programmatically. Visit our Partner page to register and get API access.",
  },
  {
    q: "What happens if my delivery is cancelled?",
    a: "If you cancel before the driver picks up your package, the delivery is cancelled at no charge. If the driver cancels, they must provide a reason and the system handles the refund process automatically.",
  },
];

function FAQSection() {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  return (
    <section className="py-20 lg:py-28 bg-white">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <p className="text-sm font-semibold uppercase tracking-wider text-primary mb-3">
            FAQ
          </p>
          <h2 className="text-3xl font-bold text-text-dark sm:text-4xl">
            Frequently Asked Questions
          </h2>
        </div>
        <div className="space-y-3">
          {faqs.map((faq, i) => (
            <div
              key={i}
              className="rounded-2xl border border-gray-100 bg-white overflow-hidden transition-all hover:shadow-sm"
            >
              <button
                onClick={() => setOpenIndex(openIndex === i ? null : i)}
                className="flex w-full items-center justify-between px-6 py-5 text-left cursor-pointer"
              >
                <span className="font-bold text-text-dark pr-4">{faq.q}</span>
                <ChevronDown
                  className={`h-5 w-5 shrink-0 text-text-muted transition-transform duration-200 ${
                    openIndex === i ? "rotate-180" : ""
                  }`}
                />
              </button>
              <div
                className={`grid transition-all duration-200 ease-in-out ${
                  openIndex === i ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
                }`}
              >
                <div className="overflow-hidden">
                  <p className="px-6 pb-5 text-sm text-text-muted leading-relaxed">
                    {faq.a}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
