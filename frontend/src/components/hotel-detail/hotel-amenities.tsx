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
          <div key={key} className="flex items-center gap-2 rounded-lg border border-border/60 px-3 py-2.5 text-sm">
            <Icon className="size-4 text-primary" />
            {t(key)}
          </div>
        );
      })}
    </div>
  );
}
