"use client";

import { useLocale, useTranslations } from "next-intl";
import { SlidersHorizontal, Star } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Slider } from "@/components/ui/slider";
import { formatMoney, providerLabel } from "@/lib/format";
import { DEFAULT_HOTEL_FILTERS, type HotelFilterOptions, type HotelFilterState } from "@/lib/filters";
import type { ProviderType } from "@/lib/api/types";

const ALL_PROVIDERS: ProviderType[] = ["TRAVELOPRO", "SABRE", "TRAVELPORT"];
const RATING_STEPS = [0, 3, 3.5, 4, 4.5];

interface HotelFiltersProps {
  options: HotelFilterOptions;
  value: HotelFilterState;
  onChange: (next: HotelFilterState) => void;
}

export function HotelFilters({ options, value, onChange }: HotelFiltersProps) {
  const t = useTranslations("Filters");
  const locale = useLocale();
  const currentMax = value.maxPrice ?? options.maxPrice;

  function toggleProvider(provider: ProviderType) {
    const next = value.providers.includes(provider)
      ? value.providers.filter((p) => p !== provider)
      : [...value.providers, provider];
    onChange({ ...value, providers: next });
  }

  function toggleRoomType(roomType: string) {
    const next = value.roomTypes.includes(roomType)
      ? value.roomTypes.filter((r) => r !== roomType)
      : [...value.roomTypes, roomType];
    onChange({ ...value, roomTypes: next });
  }

  const isDefault =
    value.maxPrice === null &&
    value.providers.length === 0 &&
    value.roomTypes.length === 0 &&
    value.minRating === 0;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle className="flex items-center gap-2 text-base">
          <SlidersHorizontal className="size-4" />
          {t("title")}
        </CardTitle>
        {!isDefault && (
          <Button variant="ghost" size="sm" onClick={() => onChange(DEFAULT_HOTEL_FILTERS)}>
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
          <Label>{t("minRating")}</Label>
          <div className="flex flex-wrap gap-2">
            {RATING_STEPS.map((rating) => (
              <Button
                key={rating}
                type="button"
                variant={value.minRating === rating ? "default" : "outline"}
                size="sm"
                onClick={() => onChange({ ...value, minRating: rating })}
              >
                {rating === 0 ? (
                  t("anyRating")
                ) : (
                  <>
                    {rating}
                    <Star className="size-3.5 fill-current" />
                  </>
                )}
              </Button>
            ))}
          </div>
        </div>

        <Separator />

        <div className="grid gap-3">
          <Label>{t("providers")}</Label>
          {ALL_PROVIDERS.map((provider) => (
            <div key={provider} className="flex items-center gap-2">
              <Checkbox
                id={`hprovider-${provider}`}
                checked={value.providers.includes(provider)}
                onCheckedChange={() => toggleProvider(provider)}
              />
              <Label htmlFor={`hprovider-${provider}`} className="text-sm font-normal">
                {providerLabel(provider)}
              </Label>
            </div>
          ))}
        </div>

        {options.roomTypes.length > 1 && (
          <>
            <Separator />
            <div className="grid gap-3">
              <Label>{t("roomTypes")}</Label>
              {options.roomTypes.map((roomType) => (
                <div key={roomType} className="flex items-center gap-2">
                  <Checkbox
                    id={`room-${roomType}`}
                    checked={value.roomTypes.includes(roomType)}
                    onCheckedChange={() => toggleRoomType(roomType)}
                  />
                  <Label htmlFor={`room-${roomType}`} className="text-sm font-normal">
                    {roomType}
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
