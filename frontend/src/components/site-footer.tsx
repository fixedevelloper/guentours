"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { Mail, Phone, MapPin, ShieldCheck } from "lucide-react";

import { Link } from "@/i18n/navigation";

const PAYMENT_BADGES = ["Visa", "Mastercard", "PayPal", "Amex"];

export function SiteFooter() {
  const t = useTranslations("Footer");
  const year = new Date().getFullYear();

  return (
    <footer className="w-full border-t border-border/40 bg-muted/30 pb-16 sm:pb-0">
      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-10 px-4 py-12 sm:px-6 sm:py-16 md:grid-cols-12 lg:px-8">
        {/* Marque + description */}
        <div className="md:col-span-4">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="relative size-10 overflow-hidden rounded-lg">
              <Image src="/logo.png" alt="GuenTours" fill className="object-contain" />
            </div>
            <span className="text-sm font-bold tracking-tight">{t("tagline")}</span>
          </Link>
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-muted-foreground">
            {t("description")}
          </p>

          <ul className="mt-6 space-y-2.5 text-sm text-muted-foreground">
            <li>
              <a href="mailto:contact@guentours.com" className="flex items-center gap-2.5 transition-colors hover:text-foreground">
                <Mail className="size-4 shrink-0" />
                contact@guentours.com
              </a>
            </li>
            <li>
              <a href="tel:+33100000000" className="flex items-center gap-2.5 transition-colors hover:text-foreground">
                <Phone className="size-4 shrink-0" />
                +33 1 00 00 00 00
              </a>
            </li>
            <li className="flex items-start gap-2.5">
              <MapPin className="mt-0.5 size-4 shrink-0" />
              <span>{t("contactAddress")}</span>
            </li>
          </ul>
        </div>

        {/* Navigation */}
        <div className="md:col-span-2">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-foreground">{t("navTitle")}</h3>
          <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
            <li><Link href="/" className="transition-colors hover:text-foreground">{t("navHome")}</Link></li>
            <li><Link href="/flights" className="transition-colors hover:text-foreground">{t("navFlights")}</Link></li>
            <li><Link href="/hotels" className="transition-colors hover:text-foreground">{t("navHotels")}</Link></li>
            <li><Link href="/dashboard" className="transition-colors hover:text-foreground">{t("navDashboard")}</Link></li>
          </ul>
        </div>

        {/* Assistance */}
        <div className="md:col-span-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-foreground">{t("supportTitle")}</h3>
          <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
            <li><a href="mailto:aide@guentours.com" className="transition-colors hover:text-foreground">{t("supportHelp")}</a></li>
            <li><a href="mailto:contact@guentours.com" className="transition-colors hover:text-foreground">{t("supportContact")}</a></li>
            <li><Link href="/legal/terms" className="transition-colors hover:text-foreground">{t("supportTerms")}</Link></li>
            <li><Link href="/legal/privacy" className="transition-colors hover:text-foreground">{t("supportPrivacy")}</Link></li>
          </ul>
        </div>

        {/* Paiement sécurisé */}
        <div className="md:col-span-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-foreground">{t("paymentTitle")}</h3>
          <div className="mt-4 flex flex-wrap gap-2">
            {PAYMENT_BADGES.map((label) => (
              <span
                key={label}
                className="rounded-md border border-border/60 bg-background px-2.5 py-1 text-[11px] font-semibold text-muted-foreground"
              >
                {label}
              </span>
            ))}
          </div>
          <div className="mt-4 flex items-center gap-2 text-xs text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" />
            <span>{t("madeWith")}</span>
          </div>
        </div>
      </div>

      {/* Barre de copyright */}
      <div className="border-t border-border/40">
        <div className="mx-auto flex max-w-7xl flex-col items-center gap-2 px-4 py-5 text-xs text-muted-foreground sm:flex-row sm:justify-between sm:px-6 lg:px-8">
          <p>© {year} GuenTours. {t("rights")}</p>
          <div className="flex items-center gap-4">
            <Link href="/legal/terms" className="transition-colors hover:text-foreground">{t("supportTerms")}</Link>
            <Link href="/legal/privacy" className="transition-colors hover:text-foreground">{t("supportPrivacy")}</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
