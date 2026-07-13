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
import { HotelSearchForm } from "@/components/search/hotel-search-form";
import { HotelResultsList } from "@/components/search/hotel-results";
import { HotelFilters } from "@/components/search/hotel-filters";
import { HotelMap } from "@/components/search/hotel-map";
import { useHotelSearch } from "@/hooks/use-search";
import { hotelSearchParamsToQuery, parseHotelSearchParams } from "@/lib/search-params";
import { DEFAULT_HOTEL_FILTERS, computeHotelFilterOptions, filterHotelOffers } from "@/lib/filters";
import type { HotelSearchParams } from "@/lib/api/types";

export default function HotelsPage() {
  return (
    <Suspense fallback={<div className="mx-auto max-w-6xl px-4 py-8"><Skeleton className="h-40 w-full" /></div>}>
      <HotelsPageContent />
    </Suspense>
  );
}

function HotelsPageContent() {
  const t = useTranslations("SearchResults");
  const tFilters = useTranslations("Filters");
  const searchParams = useSearchParams();
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [filters, setFilters] = useState(DEFAULT_HOTEL_FILTERS);
  const [hoveredKey, setHoveredKey] = useState<string | null>(null);

  const params = useMemo(() => parseHotelSearchParams(searchParams), [searchParams]);
  const query = useHotelSearch(params);

  const filterOptions = useMemo(() => computeHotelFilterOptions(query.data ?? []), [query.data]);
  const filteredOffers = useMemo(
    () => filterHotelOffers(query.data ?? [], filters),
    [query.data, filters]
  );

  const nights = params
    ? Math.max(1, Math.round((new Date(params.checkOut).getTime() - new Date(params.checkIn).getTime()) / 86_400_000))
    : 1;

  function handleSearch(next: HotelSearchParams) {
    setEditing(false);
    setFilters(DEFAULT_HOTEL_FILTERS);
    router.push(`/hotels?${hotelSearchParamsToQuery(next)}`);
  }

  function handlePinSelect(key: string) {
    setHoveredKey(key);
    document.getElementById(key)?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t("hotelsTitle")}</h1>
        <Button variant="outline" size="sm" onClick={() => setEditing((v) => !v)}>
          <SlidersHorizontal />
          {t("backToSearch")}
        </Button>
      </div>

      {editing && (
        <Card className="mb-6">
          <CardContent className="pt-6">
            <HotelSearchForm defaultValues={params ?? undefined} onSearch={handleSearch} />
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
        <div className="grid gap-6 lg:grid-cols-[300px_1fr_280px]">
          <aside className="hidden lg:block">
            <div className="lg:sticky lg:top-20">
              <HotelMap
                offers={filteredOffers}
                hoveredKey={hoveredKey}
                onHoverChange={setHoveredKey}
                onSelect={handlePinSelect}
              />
            </div>
          </aside>

          <div className="grid gap-3">
            <p className="text-sm text-muted-foreground">
              {filteredOffers.length === query.data.length
                ? t("resultsCount", { count: query.data.length })
                : tFilters("resultsFiltered", { shown: filteredOffers.length, total: query.data.length })}
            </p>
            <HotelResultsList
              offers={filteredOffers}
              nights={nights}
              params={params as HotelSearchParams}
              hoveredKey={hoveredKey}
              onHoverChange={setHoveredKey}
            />
          </div>

          <aside>
            <div className="lg:sticky lg:top-20">
              <HotelFilters options={filterOptions} value={filters} onChange={setFilters} />
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
