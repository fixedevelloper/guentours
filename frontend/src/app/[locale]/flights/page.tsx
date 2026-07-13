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
import { FlightResultsList } from "@/components/search/flight-results";
import { FlightFilters } from "@/components/search/flight-filters";
import { useFlightSearch } from "@/hooks/use-search";
import { flightSearchParamsToQuery, multiCitySearchParamsToQuery, parseFlightSearchParams } from "@/lib/search-params";
import { DEFAULT_FLIGHT_FILTERS, computeFlightFilterOptions, filterFlightOffers } from "@/lib/filters";
import type { FlightSearchParams, MultiCityFlightSearchParams } from "@/lib/api/types";

export default function FlightsPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-6xl px-4 py-8"><Skeleton className="h-40 w-full" /></div>}>
      <FlightsPageContent />
    </Suspense>
  );
}

function FlightsPageContent() {
  const t = useTranslations("SearchResults");
  const tFilters = useTranslations("Filters");
  const searchParams = useSearchParams();
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [filters, setFilters] = useState(DEFAULT_FLIGHT_FILTERS);

  const params = useMemo(() => parseFlightSearchParams(searchParams), [searchParams]);
  const query = useFlightSearch(params);

  const filterOptions = useMemo(() => computeFlightFilterOptions(query.data ?? []), [query.data]);
  const filteredOffers = useMemo(
    () => filterFlightOffers(query.data ?? [], filters),
    [query.data, filters]
  );

  function handleSearch(next: FlightSearchParams) {
    setEditing(false);
    setFilters(DEFAULT_FLIGHT_FILTERS);
    router.push(`/flights?${flightSearchParamsToQuery(next)}`);
  }

  function handleMultiCitySearch(next: MultiCityFlightSearchParams) {
    router.push(`/flights/multi-city?${multiCitySearchParamsToQuery(next)}`);
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t("flightsTitle")}</h1>
        <Button variant="outline" size="sm" onClick={() => setEditing((v) => !v)}>
          <SlidersHorizontal />
          {t("backToSearch")}
        </Button>
      </div>

      {editing && (
        <Card className="mb-6">
          <CardContent className="pt-6">
            <FlightSearchForm
              defaultValues={params ?? undefined}
              onSearch={handleSearch}
              onMultiCitySearch={handleMultiCitySearch}
            />
          </CardContent>
        </Card>
      )}

      {query.isLoading ? (
        <div className="grid gap-3">
          <Skeleton className="h-40 w-full" />
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
        <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
          <div className="grid gap-3">
            <p className="text-sm text-muted-foreground">
              {filteredOffers.length === query.data.length
                ? t("resultsCount", { count: query.data.length })
                : tFilters("resultsFiltered", { shown: filteredOffers.length, total: query.data.length })}
            </p>
            <FlightResultsList offers={filteredOffers} />
          </div>
          <aside>
            <div className="lg:sticky lg:top-20">
              <FlightFilters options={filterOptions} value={filters} onChange={setFilters} />
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
