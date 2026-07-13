"use client";

import { useLocale, useTranslations } from "next-intl";
import { PlaneTakeoff, PlaneLanding, Users } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { airlineLabel, formatDuration, formatMoney, formatTime, providerLabel } from "@/lib/format";
import type { HarmonizedFlightOffer } from "@/lib/api/types";
import { checkoutUrlForFlight } from "@/lib/checkout-url";

export function FlightResultsList({ offers }: { offers: HarmonizedFlightOffer[] }) {
  const t = useTranslations("Filters");
  const locale = useLocale();

  if (offers.length === 0) {
    return (
      <Alert>
        <AlertDescription>{t("noMatch")}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="grid gap-3">
      {offers.map((offer, index) => (
        <FlightOfferCard key={`${offer.airline}-${offer.flightNumber}-${index}`} offer={offer} locale={locale} />
      ))}
    </div>
  );
}

function FlightOfferCard({ offer, locale }: { offer: HarmonizedFlightOffer; locale: string }) {
  const t = useTranslations("SearchResults");
  const router = useRouter();
  const sortedQuotes = [...offer.quotes].sort((a, b) => Number(a.price.amount) - Number(b.price.amount));
  const cheapestOfferId = sortedQuotes[0]?.offerId;

  function handleSelect(offerId: string) {
    router.push(checkoutUrlForFlight(offer, offerId));
  }

  return (
    <Card>
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div className="grid gap-1">
          <div className="flex items-center gap-2 text-sm font-medium">
            <span className="rounded bg-muted px-1.5 py-0.5 text-xs font-semibold tracking-wide">
              {offer.airline}{offer.flightNumber}
            </span>
            <span className="text-muted-foreground">{airlineLabel(offer.airline)}</span>
            <span className="text-muted-foreground">·</span>
            <span className="text-muted-foreground">{offer.cabinClass}</span>
          </div>
          <div className="flex items-center gap-3 text-lg font-semibold">
            <span className="flex items-center gap-1">
              <PlaneTakeoff className="size-4 text-muted-foreground" />
              {offer.origin} {formatTime(offer.departureTime, locale)}
            </span>
            <span className="text-muted-foreground">→</span>
            <span className="flex items-center gap-1">
              <PlaneLanding className="size-4 text-muted-foreground" />
              {offer.destination} {formatTime(offer.arrivalTime, locale)}
            </span>
          </div>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span>
              {t("duration")}: {formatDuration(offer.departureTime, offer.arrivalTime)}
            </span>
            <span>·</span>
            <span>{t("nonstop")}</span>
            <span>·</span>
            <span className="flex items-center gap-1">
              <Users className="size-3.5" />
              {t("seatsLeft", { count: offer.seatsAvailable })}
            </span>
          </div>
        </div>
      </CardHeader>
      <CardContent className="grid gap-2">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
          {t("providersLabel")}
        </p>
        <div className="grid gap-2">
          {sortedQuotes.map((quote, i) => (
            <div key={quote.offerId}>
              {i > 0 && <Separator className="my-2" />}
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <Badge variant="outline">{providerLabel(quote.providerType)}</Badge>
                  {quote.offerId === cheapestOfferId && (
                    <Badge variant="success">{t("bestPrice")}</Badge>
                  )}
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-base font-semibold">{formatMoney(quote.price, locale)}</span>
                  <Button size="sm" onClick={() => handleSelect(quote.offerId)}>
                    {t("selectOffer")}
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
