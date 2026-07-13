"use client";

import { Suspense, useMemo } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { ChevronLeft, MapPin, Star } from "lucide-react";

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
    <Suspense fallback={<div className="mx-auto max-w-5xl px-4 py-8"><Skeleton className="h-96 w-full" /></div>}>
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
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!params || hotelOffers.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Alert variant="destructive">
          <AlertTitle>{t("notFoundTitle")}</AlertTitle>
          <AlertDescription>
            {t("notFoundBody")}
            <Button asChild variant="outline" size="sm" className="mt-3">
              <Link href="/hotels">{t("backToResults")}</Link>
            </Button>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const reference = hotelOffers[0];
  const bestRating = Math.max(...hotelOffers.map((o) => o.rating));

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <Button asChild variant="ghost" size="sm" className="mb-4 -ml-2">
        <Link href={backHref}>
          <ChevronLeft />
          {t("backToResults")}
        </Link>
      </Button>

      <div className="mb-4">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">{reference.hotelName}</h1>
        <div className="mt-1 flex items-center gap-3 text-sm text-muted-foreground">
          <span className="flex items-center gap-1">
            <MapPin className="size-4" />
            {reference.cityCode}
          </span>
          <span>·</span>
          <span className="flex items-center gap-1">
            <Star className="size-4 fill-current text-warning" />
            {bestRating.toFixed(1)}
          </span>
        </div>
      </div>

      <HotelGallery hotelName={reference.hotelName} />

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_320px]">
        <div className="grid gap-8">
          <div>
            <h2 className="mb-2 text-lg font-semibold">{t("descriptionTitle")}</h2>
            <p className="text-sm leading-relaxed text-muted-foreground">
              {t("description", { hotelName: reference.hotelName, cityCode: reference.cityCode, rating: bestRating.toFixed(1) })}
            </p>
          </div>

          <Separator />

          <div>
            <h2 className="mb-3 text-lg font-semibold">{t("facilitiesTitle")}</h2>
            <HotelAmenities hotelName={reference.hotelName} />
          </div>
        </div>

        <aside>
          <div className="lg:sticky lg:top-20">
            <HotelLocationCard hotelName={reference.hotelName} cityCode={reference.cityCode} />
          </div>
        </aside>
      </div>

      <Separator className="my-8" />

      <HotelRoomList offers={hotelOffers} nights={nights} />
    </div>
  );
}
