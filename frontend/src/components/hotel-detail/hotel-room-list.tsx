// components/hotel-detail/hotel-room-list.tsx
"use client";

import { useLocale, useTranslations } from "next-intl";
import { BedDouble, ChevronRight, Tag } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { formatMoney, providerLabel } from "@/lib/format";
import { checkoutUrlForHotel } from "@/lib/checkout-url";
import type { HarmonizedHotelOffer } from "@/lib/api/types";

export function HotelRoomList({ offers, nights }: { offers: HarmonizedHotelOffer[]; nights: number }) {
  const t = useTranslations("HotelDetail");

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-black tracking-tight text-foreground flex items-center gap-2">
        <BedDouble className="size-5 text-primary/80" />
        {t("roomsTitle") ?? "Offres et chambres disponibles"}
      </h2>
      <div className="grid gap-4.5">
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
    <Card className="overflow-hidden rounded-2xl border border-border/50 bg-card shadow-2xs">
      {/* EN-TÊTE DE LA CHAMBRE */}
      <div className="bg-slate-50/50 dark:bg-zinc-900/40 px-5 py-4 border-b border-border/40">
        <h3 className="text-base font-extrabold tracking-tight text-foreground capitalize flex items-center gap-2">
          {offer.roomType.toLowerCase()}
        </h3>
      </div>

      <CardContent className="p-0">
        <div className="divide-y divide-border/30">
          {sortedQuotes.map((quote, i) => {
            const isCheapest = quote.offerId === cheapestOfferId;
            return (
              <div 
                key={quote.offerId}
                className={cn(
                  "flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 transition-colors duration-200",
                  isCheapest ? "bg-primary/[0.01]" : "hover:bg-slate-50/20"
                )}
              >
                {/* FOURNISSEUR ET BADGES */}
                <div className="flex flex-wrap items-center gap-2">
                  <Badge 
                    variant="outline" 
                    className="rounded-lg border-border/80 bg-background px-2.5 py-0.5 text-xs font-bold text-muted-foreground"
                  >
                    {providerLabel(quote.providerType)}
                  </Badge>
                  
                  {isCheapest && (
                    <Badge 
                      className="rounded-lg bg-emerald-50 dark:bg-emerald-950/30 text-emerald-600 dark:text-emerald-400 border-none hover:bg-emerald-50 dark:hover:bg-emerald-950/30 px-2.5 py-0.5 text-xs font-black tracking-wide flex items-center gap-1 shadow-2xs"
                    >
                      <Tag className="size-3 stroke-[2.5]" />
                      {t("bestPrice") ?? "Meilleur prix"}
                    </Badge>
                  )}
                </div>

                {/* PRIX ET ACTION */}
                <div className="flex items-center justify-between sm:justify-end gap-5">
                  <div className="text-left sm:text-right space-y-0.5">
                    <div className="text-lg font-black text-foreground tracking-tight">
                      {formatMoney(quote.price, locale)}
                    </div>
                    <div className="text-[10px] text-muted-foreground font-bold tracking-wider uppercase">
                      {t("night", { count: nights })} · {t("totalStay")}
                    </div>
                  </div>

                  <Button 
                    size="sm" 
                    onClick={() => handleChoose(quote.offerId)}
                    variant={isCheapest ? "default" : "outline"}
                    className={cn(
                      "rounded-xl font-bold text-xs gap-1 py-4.5 px-4 transition-all active:scale-97 group",
                      !isCheapest && "hover:bg-accent/60"
                    )}
                  >
                    {t("selectOffer")}
                    <ChevronRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}