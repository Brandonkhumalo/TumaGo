"use client";

import { AuthProvider } from "@/lib/auth";

/**
 * Root layout for all admin routes (/tumago/admin/*).
 *
 * Wraps children in the AuthProvider so both the login page and the
 * dashboard pages can access auth state.  No Navbar or Footer from the
 * public site is rendered here — the admin section is completely
 * standalone.
 */
export default function AdminRootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AuthProvider>{children}</AuthProvider>;
}
