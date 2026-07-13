"use client";

import { useLocale, useTranslations } from "next-intl";
import { SlidersHorizontal } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Slider } from "@/components/ui/slider";
import { formatMoney, providerLabel } from "@/lib/format";
import { DEFAULT_FLIGHT_FILTERS, type FlightFilterOptions, type FlightFilterState } from "@/lib/filters";
import type { ProviderType } from "@/lib/api/types";

const ALL_PROVIDERS: ProviderType[] = ["TRAVELOPRO", "SABRE", "TRAVELPORT"];

interface FlightFiltersProps {
  options: FlightFilterOptions;
  value: FlightFilterState;
  onChange: (next: FlightFilterState) => void;
}

export function FlightFilters({ options, value, onChange }: FlightFiltersProps) {
  const t = useTranslations("Filters");
  const locale = useLocale();
  const currentMax = value.maxPrice ?? options.maxPrice;

  function toggleProvider(provider: ProviderType) {
    const next = value.providers.includes(provider)
      ? value.providers.filter((p) => p !== provider)
      : [...value.providers, provider];
    onChange({ ...value, providers: next });
  }

  function toggleAirline(airline: string) {
    const next = value.airlines.includes(airline)
      ? value.airlines.filter((a) => a !== airline)
      : [...value.airlines, airline];
    onChange({ ...value, airlines: next });
  }

  const isDefault =
    value.maxPrice === null && value.providers.length === 0 && value.airlines.length === 0;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 text-base">
          <SlidersHorizontal className="size-4" />
          {t("title")}
        </CardTitle>
        {!isDefault && (
          <Button variant="ghost" size="sm" onClick={() => onChange(DEFAULT_FLIGHT_FILTERS)}>
            {t("reset")}
          </Button>
        )}
      </CardHeader>
      <CardContent className="grid gap-6">
        <div className="grid gap-3">
          <div className="flex items-center justify-between text-sm">
            <Label>{t("maxPrice")}</Label>
            <span className="font-medium">
              {formatMoney({ amount: currentMax, currency: "EUR" }, locale)}
            </span>
          </div>
          <Slider
            min={options.minPrice}
            max={options.maxPrice}
            step={1}
            value={[currentMax]}
            onValueChange={([v]) => onChange({ ...value, maxPrice: v })}
          />
        </div>

        <Separator />

        <div className="grid gap-3">
          <Label>{t("providers")}</Label>
          {ALL_PROVIDERS.map((provider) => (
            <div key={provider} className="flex items-center gap-2">
              <Checkbox
                id={`provider-${provider}`}
                checked={value.providers.includes(provider)}
                onCheckedChange={() => toggleProvider(provider)}
              />
              <Label htmlFor={`provider-${provider}`} className="text-sm font-normal">
                {providerLabel(provider)}
              </Label>
            </div>
          ))}
        </div>

        {options.airlines.length > 1 && (
          <>
            <Separator />
            <div className="grid gap-3">
              <Label>{t("airlines")}</Label>
              {options.airlines.map((airline) => (
                <div key={airline} className="flex items-center gap-2">
                  <Checkbox
                    id={`airline-${airline}`}
                    checked={value.airlines.includes(airline)}
                    onCheckedChange={() => toggleAirline(airline)}
                  />
                  <Label htmlFor={`airline-${airline}`} className="text-sm font-normal">
                    {airline}
                  </Label>
                </div>
              ))}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
