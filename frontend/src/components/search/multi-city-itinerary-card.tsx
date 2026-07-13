"use client";

import { useLocale, useTranslations } from "next-intl";
import { PlaneLanding, PlaneTakeoff } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { airlineLabel, formatDateTime, formatMoney, providerLabel } from "@/lib/format";
import { checkoutUrlForMultiCityItinerary } from "@/lib/checkout-url";
import type { MultiCityItinerary } from "@/lib/api/types";

export function MultiCityItineraryCard({ itinerary }: { itinerary: MultiCityItinerary }) {
  const t = useTranslations("SearchResults");
  const router = useRouter();
  const locale = useLocale();

  function handleSelect() {
    router.push(checkoutUrlForMultiCityItinerary(itinerary));
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-4">
        <Badge variant="outline">{providerLabel(itinerary.providerType)}</Badge>
        <div className="flex items-center gap-3">
          <span className="text-xl font-semibold">{formatMoney(itinerary.totalPrice, locale)}</span>
          <Button size="sm" onClick={handleSelect}>
            {t("selectOffer")}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="grid gap-3">
        {itinerary.legs.map((leg, index) => (
          <div key={leg.legIndex}>
            {index > 0 && <Separator className="mb-3" />}
            <div className="grid gap-1">
              <div className="flex items-center gap-2 text-sm font-medium">
                <span className="rounded bg-muted px-1.5 py-0.5 text-xs font-semibold tracking-wide">
                  {leg.airline}{leg.flightNumber}
                </span>
                <span className="text-muted-foreground">{airlineLabel(leg.airline)}</span>
                <span className="text-muted-foreground">·</span>
                <span className="text-muted-foreground">{leg.cabinClass}</span>
              </div>
              <div className="flex flex-wrap items-center gap-3 text-sm">
                <span className="flex items-center gap-1">
                  <PlaneTakeoff className="size-4 text-muted-foreground" />
                  {leg.origin} {formatDateTime(leg.departureTime, locale)}
                </span>
                <span className="text-muted-foreground">→</span>
                <span className="flex items-center gap-1">
                  <PlaneLanding className="size-4 text-muted-foreground" />
                  {leg.destination} {formatDateTime(leg.arrivalTime, locale)}
                </span>
              </div>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
