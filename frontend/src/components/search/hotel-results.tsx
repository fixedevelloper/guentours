"use client";

import { useLocale, useTranslations } from "next-intl";
import { Star, MapPin, Building2 } from "lucide-react";

import { Link } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { cn } from "@/lib/utils";
import { formatMoney, providerLabel } from "@/lib/format";
import { hotelOfferKey } from "@/lib/filters";
import { hotelDetailUrl } from "@/lib/hotel-detail-url";
import { galleryHues } from "@/lib/hotel-mock-content";
import type { HarmonizedHotelOffer, HotelSearchParams } from "@/lib/api/types";

interface HotelResultsListProps {
  offers: HarmonizedHotelOffer[];
  nights: number;
  params: HotelSearchParams;
  hoveredKey: string | null;
  onHoverChange: (key: string | null) => void;
}

export function HotelResultsList({ offers, nights, params, hoveredKey, onHoverChange }: HotelResultsListProps) {
  const t = useTranslations("Filters");

  if (offers.length === 0) {
    return (
      <Alert>
        <AlertDescription>{t("noMatch")}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="grid gap-3">
      {offers.map((offer, index) => {
        const key = hotelOfferKey(offer, index);
        return (
          <HotelOfferCard
            key={key}
            offer={offer}
            nights={nights}
            params={params}
            id={key}
            isActive={hoveredKey === key}
            onHoverChange={(hovering) => onHoverChange(hovering ? key : null)}
          />
        );
      })}
    </div>
  );
}

function HotelOfferCard({
  offer,
  nights,
  params,
  id,
  isActive,
  onHoverChange,
}: {
  offer: HarmonizedHotelOffer;
  nights: number;
  params: HotelSearchParams;
  id: string;
  isActive: boolean;
  onHoverChange: (hovering: boolean) => void;
}) {
  const t = useTranslations("SearchResults");
  const locale = useLocale();
  const hue = galleryHues(offer.hotelName)[0];
  const minPrice = Math.min(...offer.quotes.map((q) => Number(q.price.amount)));
  const currency = offer.quotes[0].price.currency;
  const detailHref = hotelDetailUrl(offer.hotelName, params);

  return (
    <Card
      id={id}
      onMouseEnter={() => onHoverChange(true)}
      onMouseLeave={() => onHoverChange(false)}
      className={cn("scroll-mt-4 overflow-hidden transition-shadow", isActive && "ring-2 ring-primary")}
    >
      <div className="flex flex-col sm:flex-row">
        <div
          className="flex h-32 items-center justify-center text-white/70 sm:h-auto sm:w-40 sm:shrink-0"
          style={{ background: `linear-gradient(135deg, hsl(${hue} 55% 42%), hsl(${(hue + 45) % 360} 60% 28%))` }}
        >
          <Building2 className="size-8" />
        </div>

        <div className="flex flex-1 flex-col justify-between gap-3 p-4 sm:flex-row sm:items-center">
          <div className="grid gap-1.5">
            <div className="text-lg font-semibold">{offer.hotelName}</div>
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <MapPin className="size-3.5" />
                {offer.cityCode}
              </span>
              <span>·</span>
              <span>{t("roomType")}: {offer.roomType}</span>
              <span>·</span>
              <span className="flex items-center gap-1">
                <Star className="size-3.5 fill-current text-warning" />
                {offer.rating.toFixed(1)}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-1.5">
              {offer.quotes.map((quote) => (
                <Badge key={quote.offerId} variant="outline">
                  {providerLabel(quote.providerType)}
                </Badge>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between gap-4 sm:flex-col sm:items-end sm:justify-normal">
            <div className="text-right">
              <div className="text-xs text-muted-foreground">{t("night", { count: nights })} · {t("totalStay")}</div>
              <div className="text-xl font-bold">{formatMoney({ amount: minPrice, currency }, locale)}</div>
            </div>
            <Button asChild>
              <Link href={detailHref}>{t("selectOffer")}</Link>
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
}
