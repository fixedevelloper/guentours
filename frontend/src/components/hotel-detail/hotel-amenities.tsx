// components/hotel-detail/hotel-amenities.tsx
"use client";

import { useTranslations } from "next-intl";
import {
  Wifi,
  Waves,
  ParkingCircle,
  Coffee,
  Wind,
  Dumbbell,
  UtensilsCrossed,
  Sparkles,
  Bus,
  Dog,
  type LucideIcon,
} from "lucide-react";

import { pickAmenities, type AmenityKey } from "@/lib/hotel-mock-content";

const AMENITY_ICONS: Record<AmenityKey, LucideIcon> = {
  wifi: Wifi,
  pool: Waves,
  parking: ParkingCircle,
  breakfast: Coffee,
  ac: Wind,
  gym: Dumbbell,
  restaurant: UtensilsCrossed,
  spa: Sparkles,
  shuttle: Bus,
  pets: Dog,
};

export function HotelAmenities({ hotelName }: { hotelName: string }) {
  const t = useTranslations("HotelDetail.amenities");
  const amenities = pickAmenities(hotelName);

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {amenities.map((key) => {
        const Icon = AMENITY_ICONS[key];
        return (
          <div 
            key={key} 
            className="flex items-center gap-3 rounded-xl border border-border/40 bg-card p-3 text-xs sm:text-sm font-semibold text-foreground/90 shadow-2xs hover:border-primary/20 hover:bg-slate-50/50 dark:hover:bg-zinc-900/40 transition-all duration-200 select-none group"
          >
            {/* Conteneur d'icône stylisé */}
            <div className="p-2 rounded-lg bg-primary/5 text-primary group-hover:scale-110 transition-transform duration-200 shrink-0">
              <Icon className="size-4 stroke-[2.2]" />
            </div>
            <span className="truncate capitalize">{t(key)}</span>
          </div>
        );
      })}
    </div>
  );
}