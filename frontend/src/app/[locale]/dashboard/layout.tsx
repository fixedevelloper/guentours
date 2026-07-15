"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { Building2, Plane, Ticket } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { useAuth } from "@/context/auth-context";
import { Skeleton } from "@/components/ui/skeleton";
import { DashboardShell, type DashboardNavItem } from "@/components/dashboard/dashboard-shell";

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("Dashboard");
  const router = useRouter();
  const { isAuthenticated, isHydrated } = useAuth();

  useEffect(() => {
    if (isHydrated && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isHydrated, isAuthenticated, router]);

  if (!isHydrated || !isAuthenticated) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-8">
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  const navItems: DashboardNavItem[] = [
    { href: "/dashboard", label: t("navMyBookings"), icon: Ticket },
    { href: "/flights", label: t("searchFlights"), icon: Plane },
    { href: "/hotels", label: t("searchHotels"), icon: Building2 },
  ];

  return (
    <DashboardShell eyebrow={t("clientEyebrow")} navItems={navItems}>
      {children}
    </DashboardShell>
  );
}
