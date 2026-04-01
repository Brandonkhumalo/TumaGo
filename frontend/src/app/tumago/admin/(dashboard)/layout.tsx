"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import Image from "next/image";
import Link from "next/link";
import {
  LayoutDashboard,
  Truck,
  Users,
  DollarSign,
  Building2,
  Activity,
  LogOut,
  Menu,
  X,
  ChevronLeft,
} from "lucide-react";

// ── Sidebar navigation items ────────────────────────────────────────────

const NAV_ITEMS = [
  {
    label: "Dashboard",
    href: "/tumago/admin",
    icon: LayoutDashboard,
  },
  {
    label: "Deliveries",
    href: "/tumago/admin/deliveries",
    icon: Truck,
  },
  {
    label: "Drivers",
    href: "/tumago/admin/drivers",
    icon: Users,
  },
  {
    label: "Users",
    href: "/tumago/admin/users",
    icon: Users,
  },
  {
    label: "Financials",
    href: "/tumago/admin/financials",
    icon: DollarSign,
  },
  {
    label: "Partners",
    href: "/tumago/admin/partners",
    icon: Building2,
  },
  {
    label: "System Health",
    href: "/tumago/admin/system",
    icon: Activity,
  },
] as const;

// ── Layout Component ─────────────────────────────────────────────────────

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isLoading, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  // Sidebar collapsed state (desktop) and mobile open state.
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  // Close mobile sidebar on route change.
  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  // Redirect unauthenticated users to login.
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/tumago/admin/login");
    }
  }, [isLoading, isAuthenticated, router]);

  // While checking auth state, show a minimal loading screen.
  if (isLoading || !isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-100">
        <div className="animate-pulse text-text-muted text-sm">Loading...</div>
      </div>
    );
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  /** True when `href` matches the current pathname exactly. */
  function isActive(href: string) {
    // Dashboard home is an exact match; sub-pages match by prefix.
    if (href === "/tumago/admin") return pathname === "/tumago/admin";
    return pathname.startsWith(href);
  }

  // ── Sidebar content (shared between desktop and mobile) ────────────────

  function SidebarContent({ showLabels }: { showLabels: boolean }) {
    return (
      <nav className="flex-1 py-4 space-y-1 px-2">
        {NAV_ITEMS.map(({ label, href, icon: Icon }) => {
          const active = isActive(href);
          return (
            <Link
              key={href}
              href={href}
              title={label}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition ${
                active
                  ? "bg-primary/10 text-primary"
                  : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
              }`}
            >
              <Icon className="w-5 h-5 shrink-0" />
              {showLabels && <span>{label}</span>}
            </Link>
          );
        })}
      </nav>
    );
  }

  // ── Render ─────────────────────────────────────────────────────────────

  return (
    <div className="h-screen flex overflow-hidden bg-slate-100">
      {/* ── Mobile overlay ──────────────────────────────────────────── */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* ── Mobile sidebar drawer ───────────────────────────────────── */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-slate-900 flex flex-col transform transition-transform duration-200 lg:hidden ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Brand + close */}
        <div className="flex items-center justify-between h-16 px-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Image src="/tuma_go_logo.png" alt="TumaGo" width={32} height={32} className="rounded" />
            <span className="text-white font-bold text-lg">TumaGo Admin</span>
          </div>
          <button
            onClick={() => setMobileOpen(false)}
            className="text-slate-400 hover:text-white transition cursor-pointer"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <SidebarContent showLabels />

        {/* Logout (mobile) */}
        <div className="border-t border-slate-800 px-2 py-4">
          <button
            onClick={logout}
            className="flex items-center gap-3 w-full rounded-lg px-3 py-2.5 text-sm font-medium text-slate-400 hover:bg-slate-800 hover:text-red-400 transition cursor-pointer"
          >
            <LogOut className="w-5 h-5 shrink-0" />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* ── Desktop sidebar ─────────────────────────────────────────── */}
      <aside
        className={`hidden lg:flex flex-col bg-slate-900 transition-all duration-200 shrink-0 ${
          collapsed ? "w-[4.5rem]" : "w-64"
        }`}
      >
        {/* Brand + collapse toggle */}
        <div className="flex items-center h-16 px-4 border-b border-slate-800">
          <Image src="/tuma_go_logo.png" alt="TumaGo" width={32} height={32} className="rounded shrink-0" />
          {!collapsed && (
            <span className="text-white font-bold text-lg flex-1 ml-2">
              TumaGo Admin
            </span>
          )}
          <button
            onClick={() => setCollapsed((c) => !c)}
            className="text-slate-400 hover:text-white transition cursor-pointer"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            <ChevronLeft
              className={`w-5 h-5 transition-transform ${
                collapsed ? "rotate-180" : ""
              }`}
            />
          </button>
        </div>

        <SidebarContent showLabels={!collapsed} />

        {/* Logout (desktop) */}
        <div className="border-t border-slate-800 px-2 py-4">
          <button
            onClick={logout}
            title="Logout"
            className="flex items-center gap-3 w-full rounded-lg px-3 py-2.5 text-sm font-medium text-slate-400 hover:bg-slate-800 hover:text-red-400 transition cursor-pointer"
          >
            <LogOut className="w-5 h-5 shrink-0" />
            {!collapsed && <span>Logout</span>}
          </button>
        </div>
      </aside>

      {/* ── Main area (top bar + content) ───────────────────────────── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 lg:px-6 shrink-0">
          {/* Mobile hamburger */}
          <button
            onClick={() => setMobileOpen(true)}
            className="lg:hidden text-slate-600 hover:text-text-dark transition cursor-pointer"
            aria-label="Open sidebar"
          >
            <Menu className="w-6 h-6" />
          </button>

          {/* Page title — just branding on desktop since sidebar is visible */}
          <span className="hidden lg:block text-sm font-medium text-text-muted">
            Admin Dashboard
          </span>

          {/* Right side: user info + logout */}
          <div className="flex items-center gap-4">
            <span className="text-sm text-text-muted hidden sm:block">
              {user?.email}
            </span>
            <button
              onClick={logout}
              className="text-sm text-slate-500 hover:text-red-500 transition cursor-pointer"
              title="Sign out"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </header>

        {/* Content area */}
        <main className="flex-1 p-4 lg:p-6 overflow-auto">{children}</main>
      </div>
    </div>
  );
}
