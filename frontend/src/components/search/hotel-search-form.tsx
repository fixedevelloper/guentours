"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useTranslations } from "next-intl";
import { CalendarDays, MapPin, Search, Users } from "lucide-react";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { FieldShell } from "@/components/search/field-shell";
import { CounterField } from "@/components/search/counter-field";
import { LocationAutocomplete } from "@/components/search/location-autocomplete";
import { searchCitySuggestions } from "@/lib/api/geo";
import { cn } from "@/lib/utils";
import type { HotelSearchParams } from "@/lib/api/types";

const today = () => new Date().toISOString().slice(0, 10);
const tomorrow = () => new Date(Date.now() + 86_400_000).toISOString().slice(0, 10);

const schema = z.object({
  cityCode: z.string().trim().min(2, ""),
  checkIn: z.string().min(1, ""),
  checkOut: z.string().min(1, ""),
  adults: z.coerce.number().min(1).max(9),
  rooms: z.coerce.number().min(1).max(5),
});

export type HotelSearchFormValues = z.input<typeof schema>;
export type HotelSearchFormOutput = z.output<typeof schema>;

interface HotelSearchFormProps {
  defaultValues?: Partial<HotelSearchParams>;
  onSearch: (params: HotelSearchParams) => void;
  isSearching?: boolean;
}

export function HotelSearchForm({ defaultValues, onSearch, isSearching }: HotelSearchFormProps) {
  const t = useTranslations("HotelSearch");

  const form = useForm<HotelSearchFormValues, unknown, HotelSearchFormOutput>({
    resolver: zodResolver(schema),
    defaultValues: {
      cityCode: defaultValues?.cityCode ?? "",
      checkIn: defaultValues?.checkIn ?? today(),
      checkOut: defaultValues?.checkOut ?? tomorrow(),
      adults: defaultValues?.adults ?? 1,
      rooms: defaultValues?.rooms ?? 1,
    },
  });

  const adults = form.watch("adults") ?? 1;
  const rooms = form.watch("rooms") ?? 1;

  return (
    <form onSubmit={form.handleSubmit(onSearch)} className="grid gap-4">
      <div className="grid overflow-hidden rounded-2xl border border-border bg-card shadow-sm sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_auto]">
        <div className="sm:col-span-2 sm:border-r sm:border-border lg:col-span-1">
          <LocationAutocomplete
            icon={<MapPin className="size-3.5" />}
            label={t("cityCode")}
            placeholder={t("cityCodePlaceholder")}
            searchPlaceholder={t("locationSearchPlaceholder")}
            hintLabel={t("locationHint")}
            noResultsLabel={t("locationNoResults")}
            initialLabel={defaultValues?.cityCode ?? ""}
            fetchOptions={searchCitySuggestions}
            onSelect={(option) => form.setValue("cityCode", option.code, { shouldValidate: true })}
          />
        </div>

        <div className="border-t border-border sm:border-t sm:border-r lg:border-t-0">
          <FieldShell icon={<CalendarDays className="size-3.5" />} label={t("checkIn")}>
            <input
              type="date"
              min={today()}
              className="w-full bg-transparent text-sm font-semibold outline-none [color-scheme:light] dark:[color-scheme:dark]"
              {...form.register("checkIn")}
            />
          </FieldShell>
        </div>

        <div className="border-t border-border sm:border-t sm:border-r lg:border-t-0">
          <FieldShell icon={<CalendarDays className="size-3.5" />} label={t("checkOut")}>
            <input
              type="date"
              min={form.watch("checkIn") || today()}
              className="w-full bg-transparent text-sm font-semibold outline-none [color-scheme:light] dark:[color-scheme:dark]"
              {...form.register("checkOut")}
            />
          </FieldShell>
        </div>

        <div className="border-t border-border lg:border-t-0">
          <Popover>
            <PopoverTrigger asChild>
              <button type="button" className="w-full text-left transition-colors hover:bg-accent/40">
                <FieldShell icon={<Users className="size-3.5" />} label={t("guests")}>
                  <span className="truncate text-sm font-semibold">
                    {t("guestsSummary", { adults: Number(adults), rooms: Number(rooms) })}
                  </span>
                </FieldShell>
              </button>
            </PopoverTrigger>
            <PopoverContent align="end" className="w-64">
              <div className="grid divide-y divide-border">
                <CounterField
                  label={t("adults")}
                  value={Number(adults)}
                  min={1}
                  max={9}
                  onChange={(v) => form.setValue("adults", v)}
                />
                <CounterField
                  label={t("rooms")}
                  description={t("roomsDescription")}
                  value={Number(rooms)}
                  min={1}
                  max={5}
                  onChange={(v) => form.setValue("rooms", v)}
                />
              </div>
            </PopoverContent>
          </Popover>
        </div>
      </div>

      <Button
        type="submit"
        size="lg"
        disabled={isSearching}
        className={cn("w-full justify-self-end bg-gradient-to-r from-primary to-primary/80 sm:w-auto")}
      >
        <Search />
        {isSearching ? t("searching") : t("submit")}
      </Button>
    </form>
  );
}
