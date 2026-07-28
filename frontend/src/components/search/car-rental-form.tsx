"use client";

import React, { useState } from "react";
import {
    MapPin,
    Calendar,
    Clock,
    Search,
    Car,
    UserCheck,
    ArrowRightLeft,
    Check,
    Shield,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import {carSearchParamsToQuery} from "@/lib/search-params";

interface CarRentalFormProps {
    className?: string;
    onSearch?: (searchParams: CarSearchParams) => void;
}

export interface CarSearchParams {
    pickupLocation: string;
    dropoffLocation: string;
    differentDropoff: boolean;
    pickupDate: string;
    pickupTime: string;
    dropoffDate: string;
    dropoffTime: string;
    withDriver: boolean;
    driverAge25Plus: boolean;
}

export function CarRentalForm({ className = "", onSearch }: CarRentalFormProps) {
    const t = useTranslations("CarRental");
    const router = useRouter();

    const today = new Date().toISOString().split("T")[0];
    const defaultReturn = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000)
        .toISOString()
        .split("T")[0];

    const [differentDropoff, setDifferentDropoff] = useState(false);
    const [pickupLocation, setPickupLocation] = useState("");
    const [dropoffLocation, setDropoffLocation] = useState("");
    const [pickupDate, setPickupDate] = useState(today);
    const [pickupTime, setPickupTime] = useState("10:00");
    const [dropoffDate, setDropoffDate] = useState(defaultReturn);
    const [dropoffTime, setDropoffTime] = useState("10:00");
    const [withDriver, setWithDriver] = useState(false);
    const [driverAge25Plus, setDriverAge25Plus] = useState(true);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        const searchData: CarSearchParams = {
            pickupLocation,
            dropoffLocation: differentDropoff ? dropoffLocation : pickupLocation,
            differentDropoff,
            pickupDate,
            pickupTime,
            dropoffDate,
            dropoffTime,
            withDriver,
            driverAge25Plus,
        };

        if (onSearch) {
            onSearch(searchData);
        } else {
            router.push(`/car-rentals/search?${carSearchParamsToQuery(searchData)}`);
        }
    };

    return (
        <form
            onSubmit={handleSubmit}
            className={`w-full rounded-3xl bg-background/95 p-4 sm:p-6 shadow-xl border border-border/60 backdrop-blur-xl ${className}`}
        >
            {/* 1. Options rapides (Toggles) */}
            <div className="flex flex-wrap items-center gap-3 mb-5 text-xs sm:text-sm font-medium">
                <button
                    type="button"
                    onClick={() => setDifferentDropoff(!differentDropoff)}
                    className={`flex items-center gap-2 px-3.5 py-2 rounded-full border transition-all active:scale-95 ${
                        differentDropoff
                            ? "bg-primary/10 border-primary text-primary font-semibold"
                            : "bg-muted/40 border-border/60 text-muted-foreground hover:text-foreground hover:bg-muted"
                    }`}
                >
                    <ArrowRightLeft className="size-3.5" />
                    <span>{t("differentDropoff")}</span>
                </button>

                <button
                    type="button"
                    onClick={() => setWithDriver(!withDriver)}
                    className={`flex items-center gap-2 px-3.5 py-2 rounded-full border transition-all active:scale-95 ${
                        withDriver
                            ? "bg-primary/10 border-primary text-primary font-semibold"
                            : "bg-muted/40 border-border/60 text-muted-foreground hover:text-foreground hover:bg-muted"
                    }`}
                >
                    <UserCheck className="size-3.5" />
                    <span>{t("withDriver")}</span>
                </button>

                {!withDriver && (
                    <button
                        type="button"
                        onClick={() => setDriverAge25Plus(!driverAge25Plus)}
                        className={`flex items-center gap-2 px-3.5 py-2 rounded-full border transition-all active:scale-95 ${
                            driverAge25Plus
                                ? "bg-muted/60 border-border/60 text-foreground"
                                : "bg-amber-500/10 border-amber-500/30 text-amber-600 dark:text-amber-400 font-semibold"
                        }`}
                    >
                        <Shield className="size-3.5" />
                        <span>{driverAge25Plus ? t("driverAgeStandard") : t("driverAgeYoung")}</span>
                    </button>
                )}
            </div>

            {/* 2. Grille principale du formulaire */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-3 sm:gap-4 items-end">
                <div
                    className={`${
                        differentDropoff ? "lg:col-span-3" : "lg:col-span-4"
                    } flex flex-col gap-1.5`}
                >
                    <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                        <MapPin className="size-3.5 text-primary" />
                        {t("pickupLocationLabel")}
                    </label>
                    <div className="relative">
                        <input
                            type="text"
                            required
                            value={pickupLocation}
                            onChange={(e) => setPickupLocation(e.target.value)}
                            placeholder={t("pickupPlaceholder")}
                            className="w-full h-12 pl-10 pr-4 text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all placeholder:text-muted-foreground/60"
                        />
                        <Car className="absolute left-3.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                    </div>
                </div>

                {differentDropoff && (
                    <div className="lg:col-span-3 flex flex-col gap-1.5">
                        <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                            <MapPin className="size-3.5 text-primary" />
                            {t("dropoffLocationLabel")}
                        </label>
                        <div className="relative">
                            <input
                                type="text"
                                required={differentDropoff}
                                value={dropoffLocation}
                                onChange={(e) => setDropoffLocation(e.target.value)}
                                placeholder={t("dropoffPlaceholder")}
                                className="w-full h-12 pl-10 pr-4 text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all placeholder:text-muted-foreground/60"
                            />
                            <MapPin className="absolute left-3.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                        </div>
                    </div>
                )}

                <div
                    className={`${
                        differentDropoff ? "lg:col-span-3" : "lg:col-span-3"
                    } grid grid-cols-3 gap-2`}
                >
                    <div className="col-span-2 flex flex-col gap-1.5">
                        <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                            <Calendar className="size-3.5 text-primary" />
                            {t("pickupDateLabel")}
                        </label>
                        <input
                            type="date"
                            required
                            min={today}
                            value={pickupDate}
                            onChange={(e) => setPickupDate(e.target.value)}
                            className="w-full h-12 px-3 text-xs sm:text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                        />
                    </div>
                    <div className="col-span-1 flex flex-col gap-1.5">
                        <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                            <Clock className="size-3.5 text-primary" />
                            {t("timeLabel")}
                        </label>
                        <input
                            type="time"
                            required
                            value={pickupTime}
                            onChange={(e) => setPickupTime(e.target.value)}
                            className="w-full h-12 px-2 text-xs sm:text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all text-center"
                        />
                    </div>
                </div>

                <div
                    className={`${
                        differentDropoff ? "lg:col-span-3" : "lg:col-span-3"
                    } grid grid-cols-3 gap-2`}
                >
                    <div className="col-span-2 flex flex-col gap-1.5">
                        <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                            <Calendar className="size-3.5 text-primary" />
                            {t("dropoffDateLabel")}
                        </label>
                        <input
                            type="date"
                            required
                            min={pickupDate || today}
                            value={dropoffDate}
                            onChange={(e) => setDropoffDate(e.target.value)}
                            className="w-full h-12 px-3 text-xs sm:text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                        />
                    </div>
                    <div className="col-span-1 flex flex-col gap-1.5">
                        <label className="text-xs font-semibold text-muted-foreground flex items-center gap-1.5">
                            <Clock className="size-3.5 text-primary" />
                            {t("timeLabel")}
                        </label>
                        <input
                            type="time"
                            required
                            value={dropoffTime}
                            onChange={(e) => setDropoffTime(e.target.value)}
                            className="w-full h-12 px-2 text-xs sm:text-sm font-medium rounded-2xl bg-muted/30 border border-border/80 focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all text-center"
                        />
                    </div>
                </div>

                <div className={`${differentDropoff ? "lg:col-span-12" : "lg:col-span-2"}`}>
                    <button
                        type="submit"
                        className="w-full h-12 flex items-center justify-center gap-2 rounded-2xl bg-primary text-primary-foreground font-semibold text-sm hover:bg-primary/90 active:scale-[0.98] transition-all shadow-lg shadow-primary/25"
                    >
                        <Search className="size-4" />
                        <span>{t("searchBtn")}</span>
                    </button>
                </div>
            </div>
        </form>
    );
}