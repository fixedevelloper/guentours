// components/hotel-detail/hotel-gallery.tsx
"use client";

import { Building2, Sparkles, Compass, ShieldCheck } from "lucide-react";
import { galleryHues } from "@/lib/hotel-mock-content";

/**
 * Le backend n'ayant pas d'images réelles, cette galerie affiche une composition
 * abstraite et géométrique ultra-premium, déterministe selon le nom de l'hôtel.
 */
export function HotelGallery({ hotelName }: { hotelName: string }) {
  const hues = galleryHues(hotelName);

  // Liste d'icônes abstraites raffinées pour habiller les petites tuiles
  const subIcons = [
    <Sparkles key="1" className="size-5 opacity-40 mix-blend-overlay shrink-0 animate-pulse duration-3000" />,
    <Compass key="2" className="size-5 opacity-40 mix-blend-overlay shrink-0" />,
    <ShieldCheck key="3" className="size-5 opacity-40 mix-blend-overlay shrink-0" />
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 h-[420px] sm:h-[360px] w-full">
      {/* TUILE PRINCIPALE (HERO) */}
      <Tile hue={hues[0]} className="col-span-1 sm:col-span-2 row-span-2 h-full">
        <div className="absolute inset-0 bg-radial-gradient from-white/10 to-transparent pointer-events-none" />
        <div className="relative flex flex-col items-center gap-3 text-center p-6 z-10">
          <div className="p-4 rounded-2xl bg-white/10 backdrop-blur-md border border-white/10 shadow-lg">
            <Building2 className="size-10 text-white drop-shadow-md" />
          </div>
          <span className="text-[10px] font-black tracking-widest text-white/70 uppercase">
            Atmosphère Signature
          </span>
        </div>
      </Tile>

      {/* PETITES TUILES D'ATMOSPHÈRE */}
      {hues.slice(1, 4).map((hue, i) => (
        <Tile 
          key={i} 
          hue={hue} 
          className="col-span-1 h-full hidden sm:flex"
        >
          {/* Cercles luminescents floutés en arrière-plan */}
          <div className="absolute -top-12 -right-12 size-32 rounded-full bg-white/10 blur-xl pointer-events-none" />
          <div className="absolute -bottom-8 -left-8 size-24 rounded-full bg-black/10 blur-lg pointer-events-none" />
          
          <div className="z-10 p-3 rounded-xl bg-black/10 backdrop-blur-xs border border-white/5">
            {subIcons[i] || subIcons[0]}
          </div>
        </Tile>
      ))}
    </div>
  );
}

interface TileProps {
  hue: number;
  className: string;
  children: React.ReactNode;
}

function Tile({ hue, className, children }: TileProps) {
  return (
    <div
      className={`relative overflow-hidden rounded-2xl flex items-center justify-center text-white/80 transition-all duration-300 hover:scale-[1.015] hover:shadow-md ${className}`}
      style={{
        background: `linear-gradient(135deg, hsl(${hue} 55% 44%), hsl(${(hue + 35) % 360} 55% 28%))`,
      }}
    >
      {/* Texture de grille fine et élégante par-dessus le gradient */}
      <div className="absolute inset-0 bg-[radial-gradient(#ffffff_1px,transparent_1px)] [background-size:16px_16px] opacity-10 pointer-events-none" />
      {children}
    </div>
  );
}