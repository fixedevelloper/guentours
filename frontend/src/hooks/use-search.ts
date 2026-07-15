import { useQuery } from "@tanstack/react-query";

import { getFlightSeatMap, searchFlights, searchHotels, searchMultiCityFlights } from "@/lib/api/search";
import type { FlightSearchParams, HotelSearchParams, MultiCityFlightSearchParams } from "@/lib/api/types";

export function useFlightSearch(params: FlightSearchParams | null) {
  return useQuery({
    queryKey: ["flights", params],
    queryFn: () => searchFlights(params as FlightSearchParams),
    enabled: params !== null,
  });
}

export function useMultiCityFlightSearch(params: MultiCityFlightSearchParams | null) {
  return useQuery({
    queryKey: ["flights-multi-city", params],
    queryFn: () => searchMultiCityFlights(params as MultiCityFlightSearchParams),
    enabled: params !== null,
  });
}

export function useHotelSearch(params: HotelSearchParams | null) {
  return useQuery({
    queryKey: ["hotels", params],
    queryFn: () => searchHotels(params as HotelSearchParams),
    enabled: params !== null,
  });
}

export function useFlightSeatMap(offerId: string | null) {
  return useQuery({
    queryKey: ["flight-seat-map", offerId],
    queryFn: () => getFlightSeatMap(offerId as string),
    enabled: offerId !== null,
    staleTime: Infinity,
  });
}
