import Link from "next/link";
import Image from "next/image";
import { Mail, Phone, MapPin } from "lucide-react";

const footerLinks = {
  company: [
    { href: "/about", label: "About Us" },
    { href: "/partner", label: "Partner Up" },
    { href: "/contact", label: "Contact" },
  ],
  services: [
    { href: "/#how-it-works", label: "How It Works" },
    { href: "/#features", label: "Features" },
    { href: "/partner", label: "Business Solutions" },
  ],
  legal: [
    { href: "/terms", label: "Terms of Service" },
    { href: "/privacy", label: "Privacy Policy" },
    { href: "/cookies", label: "Cookie Policy" },
  ],
};

export default function Footer() {
  return (
    <footer className="bg-primary-dark text-white">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Main Footer Content */}
        <div className="grid grid-cols-1 gap-10 py-16 sm:grid-cols-2 lg:grid-cols-5">
          {/* Brand Column */}
          <div className="lg:col-span-2">
            <Link href="/" className="flex items-center gap-2 mb-4">
              <Image
                src="/tuma_go_logo.png"
                alt="TumaGo"
                width={40}
                height={40}
                className="brightness-0 invert"
              />
              <span className="text-xl font-bold text-white">
                Tuma<span className="text-primary">Go</span>
              </span>
            </Link>
            <p className="text-sm leading-relaxed text-blue-100/80 max-w-sm mb-6">
              Zimbabwe&apos;s trusted last-mile package delivery platform.
              Fast, reliable, and affordable same-city deliveries powered
              by a network of verified drivers.
            </p>
            {/* Contact Info */}
            <div className="space-y-3">
              <div className="flex items-center gap-3 text-sm text-blue-100/80">
                <Mail className="h-4 w-4 shrink-0" />
                <span>info@tumago.co.zw</span>
              </div>
              <div className="flex items-center gap-3 text-sm text-blue-100/80">
                <Phone className="h-4 w-4 shrink-0" />
                <span>078 160 3382 / 024 270 7269</span>
              </div>
              <div className="flex items-center gap-3 text-sm text-blue-100/80">
                <MapPin className="h-4 w-4 shrink-0" />
                <span>7 Martin Drive, Msasa, Harare</span>
              </div>
              <a
                href="https://wa.me/263781603382"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 text-sm text-blue-100/80 hover:text-white transition-colors"
              >
                <svg viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4 shrink-0">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
                </svg>
                <span>24/7 WhatsApp: +263 78 160 3382</span>
              </a>
            </div>
          </div>

          {/* Company Links */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-blue-100/60 mb-4">
              Company
            </h3>
            <ul className="space-y-3">
              {footerLinks.company.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-blue-100/80 hover:text-white transition-colors"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Services Links */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-blue-100/60 mb-4">
              Services
            </h3>
            <ul className="space-y-3">
              {footerLinks.services.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-blue-100/80 hover:text-white transition-colors"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Download Section */}
          <div>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-blue-100/60 mb-4">
              Get the App
            </h3>
            <p className="text-sm text-blue-100/80 mb-4">
              Download TumaGo on your Android device and start sending
              packages today.
            </p>
            <a
              href="#"
              className="inline-flex items-center gap-2 rounded-lg bg-white/10 px-4 py-2.5 text-sm font-medium text-white hover:bg-white/20 transition-colors"
            >
              <svg
                viewBox="0 0 24 24"
                fill="currentColor"
                className="h-5 w-5"
              >
                <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.198l2.807 1.626a1 1 0 0 1 0 1.73l-2.808 1.626L15.206 12l2.492-2.491zM5.864 2.658L16.802 8.99l-2.303 2.303-8.635-8.635z" />
              </svg>
              Google Play
            </a>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-white/10 py-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-sm text-blue-100/60">
            &copy; {new Date().getFullYear()} TumaGo. All rights reserved. A
            product of{" "}
            <a
              href="https://tishanyq.co.zw"
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-100/80 hover:text-white transition-colors underline underline-offset-2"
            >
              Tishanyq Digital Pvt Ltd
            </a>
          </p>
          <div className="flex items-center gap-6">
            {footerLinks.legal.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-xs text-blue-100/60 hover:text-white transition-colors"
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
