import { apiClient } from "./client";
import type {
  FlightSearchParams,
  HarmonizedFlightOffer,
  HarmonizedHotelOffer, HarmonizedPropertyOffer, HarmonizedVehicleOffer, HotelDetail,
  HotelSearchParams,
  HotelSearchResult,
  MultiCityFlightSearchParams,
  MultiCityItinerary, PropertySearchParams,
  RoomOffer,
  SeatMapResponse, VehicleSearchParams,
} from "./types";

// Must stay above the backend's own provider fan-out budget
// (app.search.flight-provider-timeout-millis / FLIGHT_SEARCH_PROVIDER_TIMEOUT_MILLIS, see
// FlightSearchService) plus headroom for network + serialization, or this fires first and aborts
// a search the backend was still legitimately working on - which is exactly what happened here:
// this was 15s (matching a backend budget of 12s at the time), the backend budget was since
// raised to 52s (.env) without updating this constant to match, so every search that took the
// backend more than 15s got aborted client-side, retried once (see QueryProvider's global
// `retry: 1`), and aborted again - two cancelled requests, no result, for a search the backend
// would have answered within its own 52s budget. 60s covers the current 52s backend budget with
// margin; if that budget changes again, this needs to move with it.
const FLIGHT_SEARCH_TIMEOUT_MS = 60_000;

export async function searchFlights(params: FlightSearchParams) {
  const { data } = await apiClient.get<HarmonizedFlightOffer[]>("/api/search/flights", {
    params,
    timeout: FLIGHT_SEARCH_TIMEOUT_MS,
  });
  return data;
}

export async function searchMultiCityFlights(params: MultiCityFlightSearchParams) {
  const { data } = await apiClient.post<MultiCityItinerary[]>("/api/search/flights/multi-city", params);
  return data;
}

export async function searchHotels(params: HotelSearchParams) {
  const { data } = await apiClient.get<HotelSearchResult>("/api/search/hotels", { params });
  return data;
}

/** Fetches an additional page of an already-run hotel search (see {@link HotelSearchResult.searchId}). */
export async function loadMoreHotels(searchId: string, pageNumber: number) {
  const { data } = await apiClient.get<HarmonizedHotelOffer[]>("/api/search/hotels/load-more", {
    params: { searchId, pageNumber },
  });
  return data;
}

export async function getFlightSeatMap(offerId: string) {
  const { data } = await apiClient.get<SeatMapResponse>("/api/search/flights/seats", { params: { offerId } });
  return data;
}
export async function getHotelDeatils(offerId: string) {
  const { data } = await apiClient.get<HotelDetail>("/api/search/hotels/details", { params: { offerId } });
  return data;
}
export async function getHotelRooms(offerId: string) {
  const { data } = await apiClient.get<RoomOffer[]>("/api/search/hotels/get-rooms", { params: { offerId } });
  return data;
}


export async function searchVehicles(params: VehicleSearchParams) {
  const { data } = await apiClient.get<HarmonizedVehicleOffer[]>("/api/search/vehicles", { params });
  return data;
}
// Ajout dans hooks/use-search.ts, sur exactement le modèle de useVehicleSearch

export async function searchProperties(params: PropertySearchParams) {
  const { data } = await apiClient.get<HarmonizedPropertyOffer[]>("/api/search/properties", { params });
  return data;
}

