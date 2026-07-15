"use client";

import { Suspense, useMemo } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { ChevronLeft, MapPin, Star, Sparkles } from "lucide-react";

import { Link } from "@/i18n/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { HotelGallery } from "@/components/hotel-detail/hotel-gallery";
import { HotelAmenities } from "@/components/hotel-detail/hotel-amenities";
import { HotelLocationCard } from "@/components/hotel-detail/hotel-location-card";
import { HotelRoomList } from "@/components/hotel-detail/hotel-room-list";
import { useHotelSearch } from "@/hooks/use-search";
import { parseHotelSearchParams, hotelSearchParamsToQuery } from "@/lib/search-params";

export default function HotelDetailPage() {
  return (
    <Suspense fallback={<HotelDetailSkeleton />}>
      <HotelDetailPageContent />
    </Suspense>
  );
}

function HotelDetailPageContent() {
  const t = useTranslations("HotelDetail");
  const routeParams = useParams<{ hotelName: string }>();
  const hotelName = decodeURIComponent(routeParams.hotelName);
  const searchParams = useSearchParams();

  const params = useMemo(() => parseHotelSearchParams(searchParams), [searchParams]);
  const query = useHotelSearch(params);

  const hotelOffers = useMemo(
    () => (query.data ?? []).filter((offer) => offer.hotelName === hotelName),
    [query.data, hotelName]
  );

  const backHref = params ? `/hotels?${hotelSearchParamsToQuery(params)}` : "/hotels";

  const nights = params
    ? Math.max(1, Math.round((new Date(params.checkOut).getTime() - new Date(params.checkIn).getTime()) / 86_400_000))
    : 1;

  if (query.isLoading) {
    return <HotelDetailSkeleton />;
  }

  if (!params || hotelOffers.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <Alert className="rounded-2xl border-border/60 bg-card p-6 shadow-xs">
          <Sparkles className="size-5 text-primary mb-2" />
          <AlertTitle className="text-lg font-bold tracking-tight text-foreground">
            {t("notFoundTitle") ?? "Hébergement introuvable"}
          </AlertTitle>
          <AlertDescription className="mt-2 text-sm text-muted-foreground leading-relaxed">
            {t("notFoundBody") ?? "Nous n'avons pas réussi à récupérer les offres pour cet hôtel. Vos filtres ou vos dates sont peut-être obsolètes."}
            <div className="mt-5">
              <Button asChild variant="default" size="sm" className="rounded-xl font-bold px-4">
                <Link href="/hotels">{t("backToResults") ?? "Retour aux résultats"}</Link>
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const reference = hotelOffers[0];
  const bestRating = Math.max(...hotelOffers.map((o) => o.rating));

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:py-10">
      
      {/* BOUTON RETOUR MINIMALISTE */}
      <Button 
        asChild 
        variant="ghost" 
        size="sm" 
        className="group mb-5 -ml-2.5 rounded-xl text-muted-foreground hover:text-foreground font-semibold text-xs gap-1 transition-all"
      >
        <Link href={backHref}>
          <ChevronLeft className="size-4 transition-transform group-hover:-translate-x-0.5" />
          {t("backToResults")}
        </Link>
      </Button>

      {/* EN-TÊTE DE L'HÔTEL */}
      <div className="mb-6 space-y-2">
        <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black tracking-tight text-foreground">
          {reference.hotelName}
        </h1>
        
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-xs sm:text-sm text-muted-foreground font-medium">
          <span className="flex items-center gap-1.5 text-foreground/80">
            <MapPin className="size-4 text-muted-foreground/60" />
            {reference.cityCode}
          </span>
          <span className="text-border">•</span>
          <span className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-amber-50 dark:bg-amber-950/20 text-amber-600 dark:text-amber-500 font-bold">
            <Star className="size-3.5 fill-current text-amber-500" />
            {bestRating.toFixed(1)}
          </span>
        </div>
      </div>

      {/* GALERIE D'IMAGES */}
      <div className="overflow-hidden rounded-2xl shadow-xs border border-border/20">
        <HotelGallery hotelName={reference.hotelName} />
      </div>

      {/* GRILLE DE PRÉSENTATION DES DÉTAILS */}
      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px] items-start">
        
        <div className="grid gap-8">
          {/* DESCRIPTION */}
          <div className="space-y-3">
            <h2 className="text-lg font-bold tracking-tight text-foreground">
              {t("descriptionTitle")}
            </h2>
            <p className="text-sm leading-relaxed text-muted-foreground/90 font-medium">
              {t("description", { 
                hotelName: reference.hotelName, 
                cityCode: reference.cityCode, 
                rating: bestRating.toFixed(1) 
              })}
            </p>
          </div>

          <Separator className="bg-border/60" />

          {/* ÉQUIPEMENTS / SERVICES */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold tracking-tight text-foreground">
              {t("facilitiesTitle")}
            </h2>
            <HotelAmenities hotelName={reference.hotelName} />
          </div>
        </div>

        {/* COLONNE LATÉRALE CARTE / LOCALISATION */}
        <aside className="lg:sticky lg:top-24">
          <div className="overflow-hidden rounded-2xl border border-border/50 bg-card p-1 shadow-2xs">
            <HotelLocationCard hotelName={reference.hotelName} cityCode={reference.cityCode} />
          </div>
        </aside>
      </div>

      <Separator className="my-10 bg-border/60" />

      {/* LISTE DES CHAMBRES ET OFFRES */}
      <div className="space-y-6">
        <HotelRoomList offers={hotelOffers} nights={nights} />
      </div>
    </div>
  );
}

// SQUELETTE DE CHARGEMENT PRÉCIS ET ESTHÉTIQUE
function HotelDetailSkeleton() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-8 space-y-6 animate-pulse">
      <div className="space-y-2">
        <Skeleton className="h-4 w-28 rounded-md" />
        <Skeleton className="h-10 w-2/3 rounded-xl" />
        <div className="flex gap-2">
          <Skeleton className="h-4 w-20 rounded-md" />
          <Skeleton className="h-4 w-12 rounded-md" />
        </div>
      </div>
      <Skeleton className="h-[350px] w-full rounded-2xl" />
      <div className="grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <div className="space-y-2">
            <Skeleton className="h-5 w-40 rounded-md" />
            <Skeleton className="h-4 w-full rounded-md" />
            <Skeleton className="h-4 w-5/6 rounded-md" />
          </div>
          <Separator />
          <div className="space-y-3">
            <Skeleton className="h-5 w-40 rounded-md" />
            <div className="grid grid-cols-2 gap-2">
              <Skeleton className="h-8 w-full rounded-lg" />
              <Skeleton className="h-8 w-full rounded-lg" />
            </div>
          </div>
        </div>
        <Skeleton className="h-48 w-full rounded-2xl" />
      </div>
    </div>
  );
}