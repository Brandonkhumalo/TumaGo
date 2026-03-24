import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "TumaGo — Last-Mile Package Delivery",
  description:
    "Fast, reliable same-city package delivery in Zimbabwe. Request a delivery, get matched with a driver, and track your package in real-time.",
  keywords: [
    "package delivery",
    "Zimbabwe",
    "Harare",
    "Bulawayo",
    "last-mile delivery",
    "courier",
    "same-day delivery",
  ],
  openGraph: {
    title: "TumaGo — Last-Mile Package Delivery",
    description:
      "Fast, reliable same-city package delivery in Zimbabwe. Request a delivery, get matched with a driver, and track your package in real-time.",
    type: "website",
    locale: "en_ZW",
    siteName: "TumaGo",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={inter.variable}>
      <body className="antialiased">{children}</body>
    </html>
  );
}
