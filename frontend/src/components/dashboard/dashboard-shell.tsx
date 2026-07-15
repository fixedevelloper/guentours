// components/dashboard/dashboard-shell.tsx
"use client";

import { useState, type ComponentType } from "react";
import { useTranslations } from "next-intl";
import { LogOut, Menu, X, Sparkles } from "lucide-react";

import { Link, usePathname } from "@/i18n/navigation";
import { useAuth } from "@/context/auth-context";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

export interface DashboardNavItem {
  href: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
}

interface DashboardShellProps {
  eyebrow: string;
  navItems: DashboardNavItem[];
  children: React.ReactNode;
}

function initials(fullName: string | undefined) {
  if (!fullName) return "?";
  return fullName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export function DashboardShell({ eyebrow, navItems, children }: DashboardShellProps) {
  const t = useTranslations("Nav");
  const pathname = usePathname();
  const { user, isAdmin, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const nav = (
    <nav className="flex-1 space-y-1.5 px-3 py-4">
      {navItems.map((item) => {
        const active = pathname === item.href;
        const Icon = item.icon;
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={() => setMobileOpen(false)}
            className={cn(
              "flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm font-bold transition-all relative group",
              active
                ? "bg-primary/[0.04] text-primary shadow-2xs border-l-2 border-primary rounded-l-none pl-3"
                : "text-muted-foreground/90 hover:bg-slate-50/80 dark:hover:bg-zinc-900/50 hover:text-foreground"
            )}
          >
            <Icon className={cn(
              "size-4 shrink-0 transition-transform duration-200 group-hover:scale-105",
              active ? "text-primary stroke-[2.2]" : "text-muted-foreground/75"
            )} />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );

  const footer = (
    <div className="p-4.5 space-y-4">
      <div className="flex items-center gap-3">
        {/* Avatar avec effet d'anneau */}
        <div className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-xs font-black text-primary border border-primary/15 shadow-2xs">
          {initials(user?.fullName)}
        </div>
        
        <div className="min-w-0 flex-1">
          <p className="truncate text-xs font-extrabold text-foreground leading-snug">
            {user?.fullName}
          </p>
          <p className="truncate text-[10px] text-muted-foreground/80 font-semibold">
            {user?.email}
          </p>
        </div>

        {isAdmin && (
          <Badge 
            variant="default" 
            className="rounded-lg bg-primary/10 text-primary border-none hover:bg-primary/10 px-1.5 py-0.5 text-[9px] font-black tracking-wide"
          >
            ADMIN
          </Badge>
        )}
      </div>

      <Button 
        variant="ghost" 
        size="sm" 
        className="w-full justify-start rounded-xl text-xs font-bold text-muted-foreground hover:text-destructive hover:bg-destructive/5 gap-2 h-9 border border-border/20 hover:border-destructive/20 transition-all"
        onClick={logout}
      >
        <LogOut className="size-3.5 stroke-[2]" />
        {t("logout") ?? "Se déconnecter"}
      </Button>
    </div>
  );

  return (
    /* 
      Changement clé ici : Utilisation d'une largeur maximale fixe (max-w-5xl ou max-w-6xl)
      pour éviter l'étirement excessif sur les moniteurs ultra-larges.
    */
    <div className="mx-auto w-full max-w-7xl px-4 py-6 md:py-8">
      
      {/* HEADER MOBILE ÉLÉGANT */}
      <div className="mb-5 sm:hidden">
        <Button 
          variant="outline" 
          size="sm" 
          onClick={() => setMobileOpen(true)}
          className="rounded-xl font-bold text-xs gap-2 border-border/70 h-9.5 px-3.5"
        >
          <Menu className="size-4 text-primary" />
          <span className="capitalize">{eyebrow.toLowerCase()}</span>
        </Button>
      </div>

      <div className="flex items-start gap-6">
        
        {/* DESKTOP SIDEBAR */}
        <aside className="hidden w-60 shrink-0 self-stretch rounded-2xl border border-border/50 bg-card sm:flex sm:flex-col shadow-2xs">
          <div className="px-5 py-4.5 border-b border-border/30 flex items-center gap-2">
            <Sparkles className="size-3.5 text-primary" />
            <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">
              {eyebrow}
            </p>
          </div>
          {nav}
          <Separator className="bg-border/30" />
          {footer}
        </aside>

        {/* MOBILE SIDEBAR & OVERLAY */}
        {mobileOpen && (
          <div className="fixed inset-0 z-50 sm:hidden">
            <div className="absolute inset-0 bg-black/40 backdrop-blur-xs transition-opacity" onClick={() => setMobileOpen(false)} />
            <div className="absolute inset-y-0 left-0 flex w-72 flex-col bg-card border-r border-border/30 shadow-2xl animate-in slide-in-from-left duration-200">
              <div className="flex items-center justify-between px-5 py-4.5 border-b border-border/30">
                <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground">
                  {eyebrow}
                </p>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="rounded-xl size-8"
                  onClick={() => setMobileOpen(false)} 
                  aria-label="Close"
                >
                  <X className="size-4" />
                </Button>
              </div>
              {nav}
              <Separator className="bg-border/30" />
              {footer}
            </div>
          </div>
        )}

        {/* CONTENU CENTRAL PRINCIPAL AVEC LARGEUR MAXIMALE CONTRÔLÉE */}
        <div className="min-w-0 flex-1">{children}</div>
      </div>
    </div>
  );
}