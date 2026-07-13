"use client";

import { useTranslations } from "next-intl";
import { Plane, Building2, ShieldCheck, RadioTower, Layers } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { FlightSearchForm } from "@/components/search/flight-search-form";
import { HotelSearchForm } from "@/components/search/hotel-search-form";
import { flightSearchParamsToQuery, hotelSearchParamsToQuery, multiCitySearchParamsToQuery } from "@/lib/search-params";
import type { FlightSearchParams, HotelSearchParams, MultiCityFlightSearchParams } from "@/lib/api/types";

export default function HomePage() {
  const t = useTranslations("Home");
  const router = useRouter();

  function handleFlightSearch(params: FlightSearchParams) {
    router.push(`/flights?${flightSearchParamsToQuery(params)}`);
  }

  function handleMultiCitySearch(params: MultiCityFlightSearchParams) {
    router.push(`/flights/multi-city?${multiCitySearchParamsToQuery(params)}`);
  }

  function handleHotelSearch(params: HotelSearchParams) {
    router.push(`/hotels?${hotelSearchParamsToQuery(params)}`);
  }

  return (
    <div>
      <section className="pointer-events-none relative overflow-hidden bg-gradient-to-b from-primary/[0.08] via-primary/[0.03] to-background">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-x-0 top-14 hidden justify-center sm:flex"
        >
          <div className="flex w-full max-w-md items-center gap-2 opacity-[0.18]">
            <span className="size-1.5 shrink-0 rounded-full bg-primary" />
            <span className="h-px flex-1 border-t border-dashed border-primary" />
            <Plane className="size-4 shrink-0 rotate-45 text-primary" />
            <span className="h-px flex-1 border-t border-dashed border-primary" />
            <span className="size-1.5 shrink-0 rounded-full bg-primary" />
          </div>
        </div>

        <div className="relative mx-auto max-w-4xl px-4 pt-16 pb-32 text-center sm:pt-20">
          <span className="mb-4 inline-flex items-center gap-1.5 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-xs font-medium tracking-wide text-primary uppercase">
            <Layers className="size-3.5" />
            {t("eyebrow")}
          </span>
          <h1 className="text-4xl font-bold tracking-tight text-balance sm:text-5xl">
            {t("heroTitle")}
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-base text-muted-foreground text-balance sm:text-lg">
            {t("heroSubtitle")}
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-4xl px-4">
        <Card className="-mt-24 border-border/60 shadow-xl shadow-primary/5 sm:-mt-28">
          <CardContent className="pt-6">
            <Tabs defaultValue="flights">
              <TabsList className="mb-6 grid w-full grid-cols-2 sm:w-auto">
                <TabsTrigger value="flights" className="gap-1.5">
                  <Plane className="size-4" />
                  {t("tabFlights")}
                </TabsTrigger>
                <TabsTrigger value="hotels" className="gap-1.5">
                  <Building2 className="size-4" />
                  {t("tabHotels")}
                </TabsTrigger>
              </TabsList>
              <TabsContent value="flights">
                <FlightSearchForm onSearch={handleFlightSearch} onMultiCitySearch={handleMultiCitySearch} />
              </TabsContent>
              <TabsContent value="hotels">
                <HotelSearchForm onSearch={handleHotelSearch} />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>

        <div className="grid grid-cols-1 gap-4 py-12 sm:grid-cols-3 sm:py-16">
          <Feature
            icon={<Layers className="size-5" />}
            title={t("featureProvidersTitle")}
            body={t("featureProvidersBody")}
          />
          <Feature
            icon={<ShieldCheck className="size-5" />}
            title={t("featureSecureTitle")}
            body={t("featureSecureBody")}
          />
          <Feature
            icon={<RadioTower className="size-5" />}
            title={t("featureTrackingTitle")}
            body={t("featureTrackingBody")}
          />
        </div>
      </div>
    </div>
  );
}

function Feature({ icon, title, body }: { icon: React.ReactNode; title: string; body: string }) {
  return (
    <div className="flex gap-3 rounded-xl border border-border/60 bg-card/50 p-4">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
        {icon}
      </span>
      <div className="grid gap-0.5">
        <h3 className="text-sm font-semibold">{title}</h3>
        <p className="text-sm text-muted-foreground">{body}</p>
      </div>
    </div>
  );
}
