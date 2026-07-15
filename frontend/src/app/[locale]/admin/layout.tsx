"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { Banknote, LayoutDashboard, Receipt, Users } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { useAuth } from "@/context/auth-context";
import { Skeleton } from "@/components/ui/skeleton";
import { DashboardShell, type DashboardNavItem } from "@/components/dashboard/dashboard-shell";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const t = useTranslations("Dashboard");
  const router = useRouter();
  const { isAuthenticated, isAdmin, isHydrated } = useAuth();

  useEffect(() => {
    if (isHydrated && !(isAuthenticated && isAdmin)) {
      router.replace("/login");
    }
  }, [isHydrated, isAuthenticated, isAdmin, router]);

  if (!isHydrated || !isAuthenticated || !isAdmin) {
    return <AdminLayoutSkeleton />;
  }

  const navItems: DashboardNavItem[] = [
    { href: "/admin", label: t("navOverview") ?? "Vue d'ensemble", icon: LayoutDashboard },
    { href: "/admin/bookings", label: t("navBookings") ?? "Réservations", icon: Receipt },
    { href: "/admin/users", label: t("navUsers") ?? "Utilisateurs", icon: Users },
    { href: "/admin/commission", label: t("navCommission") ?? "Commissions", icon: Banknote },
  ];

  return (
    <DashboardShell eyebrow={t("adminEyebrow") ?? "Administration"} navItems={navItems}>
      {children}
    </DashboardShell>
  );
}

// SQUELETTE DE STRUCTURE DE PANNEAU D'ADMINISTRATION
function AdminLayoutSkeleton() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 animate-pulse space-y-6">
      {/* Fil d'Ariane / En-tête de chargement */}
      <div className="space-y-2">
        <Skeleton className="h-4 w-32 rounded-md" />
        <Skeleton className="h-8 w-48 rounded-xl" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-[220px_1fr] gap-8">
        {/* Barre latérale factice */}
        <div className="space-y-2.5 hidden md:block">
          <Skeleton className="h-10 w-full rounded-xl" />
          <Skeleton className="h-10 w-full rounded-xl" />
          <Skeleton className="h-10 w-full rounded-xl" />
          <Skeleton className="h-10 w-full rounded-xl" />
        </div>

        {/* Espace de contenu principal factice */}
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Skeleton className="h-28 w-full rounded-2xl" />
            <Skeleton className="h-28 w-full rounded-2xl" />
            <Skeleton className="h-28 w-full rounded-2xl" />
          </div>
          <Skeleton className="h-80 w-full rounded-2xl" />
        </div>
      </div>
    </div>
  );
}