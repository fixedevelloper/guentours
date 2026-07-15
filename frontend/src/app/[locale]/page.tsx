"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Plane, Building2, ShieldCheck, RadioTower, Layers, ArrowRight } from "lucide-react";

import { useRouter } from "@/i18n/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { FlightSearchForm } from "@/components/search/flight-search-form";
import { HotelSearchForm } from "@/components/search/hotel-search-form";
import { flightSearchParamsToQuery, hotelSearchParamsToQuery, multiCitySearchParamsToQuery } from "@/lib/search-params";
import type { FlightSearchParams, HotelSearchParams, MultiCityFlightSearchParams } from "@/lib/api/types";

// Configuration des contenus des Heros (Textes et Images de fond)
const HERO_CONFIGS = {
  flights: {
    badge: "Flights",
    bgImage: "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?auto=format&fit=crop&w=1920&q=80",
  },
  hotels: {
    badge: "Hotels",
    bgImage: "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1920&q=80",
  }
};

export default function HomePage() {
  const t = useTranslations("Home");
  const router = useRouter();
  
  // État pour piloter l'onglet actif et synchroniser le Hero
  const [activeTab, setActiveTab] = useState<"flights" | "hotels">("flights");

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
    <div className="relative min-h-screen">
      
      {/* SECTION HERO UNIFIÉE (Ajustée pour le responsive) */}
      <section className="relative w-full overflow-hidden bg-zinc-800 text-white pt-20 pb-32 sm:pt-28 sm:pb-40 flex items-center justify-center">
        
        {/* Images de fond avec opacité et transitions douces */}
        {Object.entries(HERO_CONFIGS).map(([key, config]) => (
          <div
            key={key}
            className={`absolute inset-0 transition-opacity duration-1000 ease-in-out ${
              activeTab === key ? "opacity-40 scale-100" : "opacity-0 scale-105"
            } transform duration-[1500ms]`}
            style={{
              backgroundImage: `url('${config.bgImage}')`,
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          />
        ))}

        {/* Overlay progressif sombre */}
     <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-900/40 to-neutral-950/80" />
        {/* Contenu textuel dynamique du Hero */}
        <div className="relative z-10 mx-auto w-full max-w-4xl px-4 text-center">
          
          {/* Badge Dynamique */}
          <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 backdrop-blur-md px-3 py-0.5 sm:px-3.5 sm:py-1 text-[11px] sm:text-xs font-semibold tracking-wide text-white uppercase transition-all duration-300">
            {activeTab === "flights" ? <Plane className="size-3 sm:size-3.5" /> : <Building2 className="size-3 sm:size-3.5" />}
            {activeTab === "flights" ? t("eyebrow") : t("eyebrowHotels") || "Stay"}
          </span>

          {/* Titre Dynamique (Rendu fluide sans saut de mise en page grâce à grid/CSS) */}
          <div className="mt-4 sm:mt-6 grid grid-cols-1">
            <h1 className="col-start-1 row-start-1 text-3xl font-black tracking-tight text-balance sm:text-5xl md:text-6xl text-white drop-shadow-md">
              <span key={activeTab} className="inline-block animate-fade-in-up">
                {activeTab === "flights" ? t("heroTitle") : t("heroTitleHotels") || "Find Your Dream Stay"}
              </span>
            </h1>
          </div>

          {/* Sous-titre Dynamique */}
          <div className="mt-3 sm:mt-4 grid grid-cols-1">
            <p className="col-start-1 row-start-1 mx-auto max-w-2xl text-sm text-zinc-200 text-balance sm:text-base md:text-lg leading-relaxed drop-shadow">
              <span key={activeTab} className="inline-block animate-fade-in-up delay-75">
                {activeTab === "flights" ? t("heroSubtitle") : t("heroSubtitleHotels") || "Explore top rated luxury accommodations worldwide."}
              </span>
            </p>
          </div>
        </div>
      </section>

      {/* FORMULAIRE DE RECHERCHE ET RESTE DE LA PAGE */}
      <div className="relative mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        
        {/* Card de recherche chevauchant le Hero (Ajustement du décalage négatif sur mobile) */}
        <Card className="-mt-16 sm:-mt-24 border-border/40 bg-background/80 backdrop-blur-2xl shadow-2xl shadow-primary/5 transition-all duration-300 hover:border-border/60">
          <CardContent className="p-4 sm:p-6 md:p-8">
            
            <Tabs 
              value={activeTab} 
              onValueChange={(val) => setActiveTab(val as "flights" | "hotels")} 
              className="w-full"
            >
              {/* TabsList fluide sur mobile (défilement horizontal automatique si nécessaire) */}
              <TabsList className="mb-4 sm:mb-6 flex h-auto items-center justify-start rounded-xl bg-muted/60 p-1 text-muted-foreground w-full sm:w-max overflow-x-auto">
                <TabsTrigger 
                  value="flights" 
                  className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-lg px-3 py-2 sm:px-5 text-xs sm:text-sm font-semibold transition-all data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-md"
                >
                  <Plane className="size-3.5 sm:size-4" />
                  {t("tabFlights")}
                </TabsTrigger>
                <TabsTrigger 
                  value="hotels" 
                  className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 rounded-lg px-3 py-2 sm:px-5 text-xs sm:text-sm font-semibold transition-all data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-md"
                >
                  <Building2 className="size-3.5 sm:size-4" />
                  {t("tabHotels")}
                </TabsTrigger>
              </TabsList>
              
              <TabsContent value="flights" className="focus-visible:outline-none mt-0">
                <FlightSearchForm onSearch={handleFlightSearch} onMultiCitySearch={handleMultiCitySearch} />
              </TabsContent>
              <TabsContent value="hotels" className="focus-visible:outline-none mt-0">
                <HotelSearchForm onSearch={handleHotelSearch} />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>

        {/* Section de réassurance / Features (Passe de 1 col à 3 cols de façon fluide) */}
        <div className="grid grid-cols-1 gap-4 sm:gap-6 py-12 sm:py-20 lg:py-24 md:grid-cols-3">
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

// Composant Feature entièrement adaptatif
function Feature({ icon, title, body }: { icon: React.ReactNode; title: string; body: string }) {
  return (
    <div className="group flex flex-col gap-3 rounded-2xl border border-border/40 bg-card/40 backdrop-blur-sm p-5 transition-all duration-300 hover:bg-card/80 hover:shadow-lg hover:shadow-primary/[0.02] md:hover:-translate-y-1">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors duration-300 group-hover:bg-primary group-hover:text-primary-foreground">
        {icon}
      </span>
      <div className="space-y-1">
        <h3 className="text-sm sm:text-base font-bold tracking-tight flex items-center gap-1 group-hover:text-primary transition-colors duration-300">
          {title}
          <ArrowRight className="size-3.5 opacity-0 -translate-x-2 transition-all duration-300 md:group-hover:opacity-100 md:group-hover:translate-x-0" />
        </h3>
        <p className="text-xs sm:text-sm text-muted-foreground/80 leading-relaxed">{body}</p>
      </div>
    </div>
  );
}