"use client";

import { useLocale, useTranslations } from "next-intl";
import { Building2, PlaneTakeoff, PlaneLanding } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { airlineLabel, formatDate, formatDateTime, formatMoney, providerLabel } from "@/lib/format";
import type { OfferSummary } from "@/lib/offer-summary";

export function OfferSummaryCard({ offer }: { offer: OfferSummary }) {
  const t = useTranslations("Checkout");
  const locale = useLocale();

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("summaryTitle")}</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {offer.offerType === "FLIGHT" ? (
          <>
            <div className="flex items-center gap-2 text-sm font-medium">
              <span className="rounded bg-muted px-1.5 py-0.5 text-xs font-semibold tracking-wide">
                {offer.airline}{offer.flightNumber}
              </span>
              <span className="text-muted-foreground">{airlineLabel(offer.airline)}</span>
              <span className="text-muted-foreground">·</span>
              <span className="text-muted-foreground">{offer.cabinClass}</span>
            </div>
            <div className="grid gap-1 text-sm">
              <span className="flex items-center gap-2">
                <PlaneTakeoff className="size-4 text-muted-foreground" />
                {offer.origin} — {formatDateTime(offer.departureTime, locale)}
              </span>
              <span className="flex items-center gap-2">
                <PlaneLanding className="size-4 text-muted-foreground" />
                {offer.destination} — {formatDateTime(offer.arrivalTime, locale)}
              </span>
            </div>
          </>
        ) : offer.offerType === "MULTI_CITY_FLIGHT" ? (
          <div className="grid gap-2 text-sm">
            {offer.legs.map((leg) => (
              <div key={leg.legIndex} className="grid gap-0.5 rounded-lg border border-border/60 p-2">
                <span className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                  <span className="rounded bg-muted px-1.5 py-0.5 font-semibold tracking-wide text-foreground">
                    {leg.airline}{leg.flightNumber}
                  </span>
                  {airlineLabel(leg.airline)}
                </span>
                <span className="flex items-center gap-2">
                  <PlaneTakeoff className="size-3.5 text-muted-foreground" />
                  {leg.origin} — {formatDateTime(leg.departureTime, locale)}
                </span>
                <span className="flex items-center gap-2">
                  <PlaneLanding className="size-3.5 text-muted-foreground" />
                  {leg.destination} — {formatDateTime(leg.arrivalTime, locale)}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <>
            <div className="flex items-center gap-2 text-sm font-medium">
              <Building2 className="size-4 text-muted-foreground" />
              {offer.hotelName}
            </div>
            <div className="grid gap-1 text-sm text-muted-foreground">
              <span>{offer.cityCode} · {offer.roomType}</span>
              <span>
                {formatDate(offer.checkIn, locale)} → {formatDate(offer.checkOut, locale)}
              </span>
            </div>
          </>
        )}

        <Separator />

        <div className="flex items-center justify-between">
          <Badge variant="outline">{providerLabel(offer.providerType)}</Badge>
          <span className="text-xl font-semibold">
            {formatMoney({ amount: offer.amount, currency: offer.currency }, locale)}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
