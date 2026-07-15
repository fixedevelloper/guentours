import { apiClient } from "./client";
import type {
  FlightSearchParams,
  HarmonizedFlightOffer,
  HarmonizedHotelOffer,
  HotelSearchParams,
  MultiCityFlightSearchParams,
  MultiCityItinerary,
  SeatMapResponse,
} from "./types";

export async function searchFlights(params: FlightSearchParams) {
  const { data } = await apiClient.get<HarmonizedFlightOffer[]>("/api/search/flights", { params });
  return data;
}

export async function searchMultiCityFlights(params: MultiCityFlightSearchParams) {
  const { data } = await apiClient.post<MultiCityItinerary[]>("/api/search/flights/multi-city", params);
  return data;
}

export async function searchHotels(params: HotelSearchParams) {
  const { data } = await apiClient.get<HarmonizedHotelOffer[]>("/api/search/hotels", { params });
  return data;
}

export async function getFlightSeatMap(offerId: string) {
  const { data } = await apiClient.get<SeatMapResponse>("/api/search/flights/seats", { params: { offerId } });
  return data;
}
