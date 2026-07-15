"use client";

import { useLocale, useTranslations } from "next-intl";
import { Plane, Users, Clock, Award, ShieldCheck, ChevronRight, ExternalLink } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
      <Alert className="rounded-2xl border-dashed">
        <AlertDescription className="text-center py-4 text-muted-foreground font-medium">
          {t("noMatch")}
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="grid gap-4">
      {offers.map((offer, index) => (
        <FlightOfferCard key={`${offer.airline}-${offer.flightNumber}-${index}`} offer={offer} locale={locale} />
      ))}
    </div>
  );
}

function FlightOfferCard({ offer, locale }: { offer: HarmonizedFlightOffer; locale: string }) {
  const t = useTranslations("SearchResults");
  const router = useRouter();
  
  // Tri des offres par prix croissant
  const sortedQuotes = [...offer.quotes].sort((a, b) => Number(a.price.amount) - Number(b.price.amount));
  const cheapestQuote = sortedQuotes[0];
  const cheapestOfferId = cheapestQuote?.offerId;

  function handleSelect(offerId: string) {
    router.push(checkoutUrlForFlight(offer, offerId));
  }

  return (
    <Card className="overflow-hidden border-border/50 bg-background transition-all duration-200 hover:shadow-md hover:border-primary/30 rounded-2xl">
      <div className="grid lg:grid-cols-[1fr_260px]">
        
        {/* SECTION GAUCHE : DÉTAILS DU VOL */}
        <div className="p-5 sm:p-6 flex flex-col justify-between gap-6 border-b lg:border-b-0 lg:border-r border-border/50">
          
          {/* Header : Compagnie & Cabine */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              {/* Logo de la compagnie (Stylisé) */}
              <div className="flex size-10 items-center justify-center rounded-xl bg-slate-100 dark:bg-zinc-900 border border-border/50 text-sm font-bold tracking-wider text-primary">
                {offer.airline.substring(0, 2)}
              </div>
              <div>
                <h4 className="font-semibold text-foreground leading-none">
                  {airlineLabel(offer.airline)}
                </h4>
                <p className="text-xs text-muted-foreground mt-1">
                  {offer.airline} {offer.flightNumber} • <span className="font-medium text-primary/80">{offer.cabinClass}</span>
                </p>
              </div>
            </div>

            {/* Alerte places restantes si critique */}
            {offer.seatsAvailable <= 5 && (
              <Badge variant="destructive" className="rounded-full bg-red-500/10 text-red-500 border-none px-2.5 py-1 text-[10px] font-bold">
                <Users className="mr-1 size-3" />
                {t("seatsLeft", { count: offer.seatsAvailable })}
              </Badge>
            )}
          </div>

          {/* Ligne graphique de vol (Frise chronologique moderne) */}
          <div className="flex items-center justify-between gap-4 py-2">
            {/* Départ */}
            <div className="flex flex-col min-w-[70px]">
              <span className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
                {formatTime(offer.departureTime, locale)}
              </span>
              <span className="text-sm font-bold text-muted-foreground uppercase">{offer.origin}</span>
            </div>

            {/* Connecteur de vol central */}
            <div className="relative flex-1 flex flex-col items-center justify-center px-2 max-w-[220px]">
              <span className="text-[11px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
                <Clock className="size-3" />
                {formatDuration(offer.departureTime, offer.arrivalTime)}
              </span>
              
              {/* Ligne de vol */}
              <div className="relative w-full h-0.5 bg-border rounded-full flex items-center justify-center">
                <div className="absolute size-1.5 rounded-full bg-primary -left-0.5" />
                <Plane className="size-4 text-primary bg-background px-0.5 rotate-90 z-10" />
                <div className="absolute size-1.5 rounded-full bg-primary -right-0.5" />
              </div>
              
              <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-500 tracking-wider uppercase mt-1">
                {t("nonstop")}
              </span>
            </div>

            {/* Arrivée */}
            <div className="flex flex-col text-right min-w-[70px]">
              <span className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
                {formatTime(offer.arrivalTime, locale)}
              </span>
              <span className="text-sm font-bold text-muted-foreground uppercase">{offer.destination}</span>
            </div>
          </div>

          {/* Assurance / Confiance */}
          <div className="flex items-center gap-4 text-xs text-muted-foreground border-t border-border/30 pt-4">
            <span className="flex items-center gap-1 text-emerald-600 dark:text-emerald-500 font-medium">
              <ShieldCheck className="size-3.5" />
              Billet modifiable ou remboursable
            </span>
          </div>
        </div>

        {/* SECTION DROITE : ESPACE DE RÉSERVATION DE TYPE PORTAIL */}
        <div className="bg-slate-50/40 dark:bg-zinc-900/10 p-5 sm:p-6 flex flex-col justify-between gap-6">
          
          {/* Bloc Prix d'Appel (Le moins cher) */}
          <div className="flex lg:flex-col justify-between items-end lg:items-stretch gap-2">
            <div>
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest block mb-0.5">
                Meilleur prix
              </span>
              <p className="text-3xl font-black text-foreground tracking-tight leading-none">
                {cheapestQuote && formatMoney(cheapestQuote.price, locale)}
              </p>
              <span className="text-[10px] text-muted-foreground block mt-1">via {providerLabel(cheapestQuote?.providerType)}</span>
            </div>

            {/* BOUTON SELECTIONNER PRINCIPAL ULTRA-MODERNE */}
            {cheapestQuote && (
              <Button 
                onClick={() => handleSelect(cheapestQuote.offerId)}
                className="w-auto lg:w-full py-5 rounded-xl text-sm font-extrabold bg-primary text-primary-foreground hover:bg-primary/95 shadow-md shadow-primary/10 transition-transform active:scale-[0.98] flex items-center justify-center gap-1.5"
              >
                Voir l'offre
                <ChevronRight className="size-4" />
              </Button>
            )}
          </div>

          {/* Alternatives de prestataires (si + de 1) */}
          {sortedQuotes.length > 1 && (
            <div className="space-y-2.5">
              <Separator className="bg-border/40" />
              <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">
                Autres prestataires ({sortedQuotes.length - 1})
              </p>
              
              <div className="grid gap-2">
                {sortedQuotes.slice(1).map((quote) => (
                  <div 
                    key={quote.offerId} 
                    onClick={() => handleSelect(quote.offerId)}
                    className="flex items-center justify-between p-1.5 rounded-lg hover:bg-muted/40 cursor-pointer transition-colors border border-transparent hover:border-border/30"
                  >
                    <span className="text-xs font-semibold text-muted-foreground">
                      {providerLabel(quote.providerType)}
                    </span>
                    <div className="flex items-center gap-1">
                      <span className="text-xs font-bold text-foreground">
                        {formatMoney(quote.price, locale)}
                      </span>
                      <ExternalLink className="size-3 text-muted-foreground" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

        </div>

      </div>
    </Card>
  );
}