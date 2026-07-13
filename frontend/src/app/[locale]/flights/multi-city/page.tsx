"use client";

import { Suspense, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { SlidersHorizontal } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertTitle } from "@/components/ui/alert";
import { FlightSearchForm } from "@/components/search/flight-search-form";
import { MultiCityItineraryCard } from "@/components/search/multi-city-itinerary-card";
import { useMultiCityFlightSearch } from "@/hooks/use-search";
import { flightSearchParamsToQuery, multiCitySearchParamsToQuery, parseMultiCitySearchParams } from "@/lib/search-params";
import type { FlightSearchParams, MultiCityFlightSearchParams } from "@/lib/api/types";

export default function MultiCityFlightsPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-6xl px-4 py-8"><Skeleton className="h-40 w-full" /></div>}>
      <MultiCityFlightsPageContent />
    </Suspense>
  );
}

function MultiCityFlightsPageContent() {
  const t = useTranslations("SearchResults");
  const searchParams = useSearchParams();
  const router = useRouter();
  const [editing, setEditing] = useState(false);

  const params = useMemo(() => parseMultiCitySearchParams(searchParams), [searchParams]);
  const query = useMultiCityFlightSearch(params);

  function handleMultiCitySearch(next: MultiCityFlightSearchParams) {
    setEditing(false);
    router.push(`/flights/multi-city?${multiCitySearchParamsToQuery(next)}`);
  }

  function handleFlightSearch(next: FlightSearchParams) {
    router.push(`/flights?${flightSearchParamsToQuery(next)}`);
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t("multiCityTitle")}</h1>
        <Button variant="outline" size="sm" onClick={() => setEditing((v) => !v)}>
          <SlidersHorizontal />
          {t("backToSearch")}
        </Button>
      </div>

      {editing && (
        <Card className="mb-6">
          <CardContent className="pt-6">
            <FlightSearchForm
              defaultLegs={params?.legs}
              onSearch={handleFlightSearch}
              onMultiCitySearch={handleMultiCitySearch}
            />
          </CardContent>
        </Card>
      )}

      {query.isLoading ? (
        <div className="grid gap-3">
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      ) : query.isError ? (
        <Alert variant="destructive">
          <AlertTitle>{t("noResults")}</AlertTitle>
        </Alert>
      ) : !query.data || query.data.length === 0 ? (
        <Alert>
          <AlertTitle>{t("noResults")}</AlertTitle>
        </Alert>
      ) : (
        <div className="grid gap-4">
          <p className="text-sm text-muted-foreground">{t("resultsCount", { count: query.data.length })}</p>
          {query.data.map((itinerary, index) => (
            <MultiCityItineraryCard key={`${itinerary.providerType}-${index}`} itinerary={itinerary} />
          ))}
        </div>
      )}
    </div>
  );
}
