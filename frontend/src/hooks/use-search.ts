import { useQuery } from "@tanstack/react-query";

import { searchFlights, searchHotels, searchMultiCityFlights } from "@/lib/api/search";
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
