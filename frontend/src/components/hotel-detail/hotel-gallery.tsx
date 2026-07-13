import { Building2, ImageOff } from "lucide-react";

import { galleryHues } from "@/lib/hotel-mock-content";

/**
 * The backend has no photo URLs (ProviderMockSupport only generates name/city/room/rating),
 * so this renders stylized gradient tiles instead of pretending to show real photography -
 * same hotel always gets the same hues (deterministic from its name).
 */
export function HotelGallery({ hotelName }: { hotelName: string }) {
  const hues = galleryHues(hotelName);

  return (
    <div className="grid grid-cols-4 grid-rows-2 gap-2 overflow-hidden rounded-xl" style={{ height: 360 }}>
      <Tile hue={hues[0]} className="col-span-2 row-span-2">
        <Building2 className="size-10" />
      </Tile>
      {hues.slice(1).map((hue, i) => (
        <Tile key={i} hue={hue} className="col-span-1 row-span-1">
          <ImageOff className="size-5" />
        </Tile>
      ))}
    </div>
  );
}

function Tile({ hue, className, children }: { hue: number; className: string; children: React.ReactNode }) {
  return (
    <div
      className={`relative flex items-center justify-center text-white/70 ${className}`}
      style={{
        background: `linear-gradient(135deg, hsl(${hue} 55% 42%), hsl(${(hue + 45) % 360} 60% 28%))`,
      }}
    >
      {children}
    </div>
  );
}
