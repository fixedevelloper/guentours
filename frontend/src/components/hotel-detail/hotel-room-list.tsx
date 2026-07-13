"use client";

import { useLocale, useTranslations } from "next-intl";
import { BedDouble } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatMoney, providerLabel } from "@/lib/format";
import { checkoutUrlForHotel } from "@/lib/checkout-url";
import type { HarmonizedHotelOffer } from "@/lib/api/types";

export function HotelRoomList({ offers, nights }: { offers: HarmonizedHotelOffer[]; nights: number }) {
  const t = useTranslations("HotelDetail");

  return (
    <div className="grid gap-4">
      <h2 className="text-lg font-semibold">{t("roomsTitle")}</h2>
      <div className="grid gap-3">
        {offers.map((offer, index) => (
          <RoomCard key={`${offer.roomType}-${index}`} offer={offer} nights={nights} />
        ))}
      </div>
    </div>
  );
}

function RoomCard({ offer, nights }: { offer: HarmonizedHotelOffer; nights: number }) {
  const t = useTranslations("SearchResults");
  const locale = useLocale();
  const router = useRouter();
  const sortedQuotes = [...offer.quotes].sort((a, b) => Number(a.price.amount) - Number(b.price.amount));
  const cheapestOfferId = sortedQuotes[0]?.offerId;

  function handleChoose(offerId: string) {
    router.push(checkoutUrlForHotel(offer, offerId));
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <BedDouble className="size-4 text-muted-foreground" />
          {offer.roomType}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2">
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
                  <div className="text-right">
                    <div className="text-base font-semibold">{formatMoney(quote.price, locale)}</div>
                    <div className="text-xs text-muted-foreground">
                      {t("night", { count: nights })} · {t("totalStay")}
                    </div>
                  </div>
                  <Button size="sm" onClick={() => handleChoose(quote.offerId)}>
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
