// Types mirroring the guentours-api backend contracts (com.guentours.* DTOs) 1:1
// so the frontend never has to guess field names or enum values.

export type JourneyType = "ONE_WAY" | "ROUND_TRIP" | "MULTI_CITY";

export type ProviderType = "TRAVELOPRO" | "SABRE" | "TRAVELPORT";

export type PassengerType = "ADULT" | "CHILD" | "INFANT";

export type OfferType = "FLIGHT" | "HOTEL";

export type BookingStatus =
  | "PENDING_PAYMENT"
  | "DEPOSIT_PAID"
  | "PAID"
  | "CONFIRMING"
  | "CONFIRMED"
  | "FAILED"
  | "CANCELLED";

export type PaymentStatus = "PENDING" | "SUCCEEDED" | "FAILED";

export type CabinClass = "ECONOMY" | "PREMIUM_ECONOMY" | "BUSINESS" | "FIRST";

export type PaymentPlan = "PAY_NOW" | "PAY_LATER";

export type PaymentMethod = "CARD" | "MTN_MOBILE_MONEY" | "ORANGE_MONEY";

export interface Money {
  amount: string | number;
  currency: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: string[];
}

// ---------- Search ----------

export interface FlightSearchParams {
  origin: string;
  destination: string;
  departureDate: string; // ISO date (yyyy-MM-dd)
  returnDate?: string;
  adults?: number;
  children?: number;
  infants?: number;
  journeyType?: JourneyType;
  cabinClass?: CabinClass;
  currency?: string;
}

export interface HotelSearchParams {
  cityCode: string;
  checkIn: string;
  checkOut: string;
  adults?: number;
  rooms?: number;
}

export interface FlightLeg {
  origin: string;
  destination: string;
  departureDate: string; // ISO date (yyyy-MM-dd)
}

export interface MultiCityFlightSearchParams {
  legs: FlightLeg[];
  adults?: number;
  children?: number;
  infants?: number;
  cabinClass?: CabinClass;
  currency?: string;
}

export interface MultiCityItineraryLeg {
  legIndex: number;
  airline: string;
  flightNumber: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  cabinClass: string;
  offerId: string;
}

export interface MultiCityItinerary {
  providerType: ProviderType;
  totalPrice: Money;
  legs: MultiCityItineraryLeg[];
}

// ---------- Geo (reference data) ----------

export interface AirportOption {
  airportCode: string;
  airportName: string;
  city: string;
  country: string;
}

export interface HotelCityOption {
  cityName: string;
  countryName: string;
  latitude: number;
  longitude: number;
}

export interface ProviderQuote {
  offerId: string;
  providerType: ProviderType;
  price: Money;
}

export interface HarmonizedFlightOffer {
  airline: string;
  flightNumber: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  cabinClass: string;
  seatsAvailable: number;
  bestOfferId: string;
  quotes: ProviderQuote[];
}

export interface HarmonizedHotelOffer {
  hotelName: string;
  cityCode: string;
  roomType: string;
  checkIn: string;
  checkOut: string;
  rating: number;
  bestOfferId: string;
  quotes: ProviderQuote[];
}

// ---------- Booking ----------

export interface TravelerRequest {
  fullName: string;
  dateOfBirth?: string;
  passportNumber?: string;
  type: PassengerType;
}

export interface CheckoutRequest {
  offerId: string;
  offerType: OfferType;
  contactEmail: string;
  contactFullName: string;
  contactPhone?: string;
  travelers: TravelerRequest[];
  paymentPlan?: PaymentPlan;
}

export interface MultiCityCheckoutRequest {
  legOfferIds: string[];
  contactEmail: string;
  contactFullName: string;
  contactPhone?: string;
  travelers: TravelerRequest[];
  paymentPlan?: PaymentPlan;
}

export interface BookingFlightLeg {
  legIndex: number;
  airline: string;
  flightNumber: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
}

export interface BookingResponse {
  id: string;
  status: BookingStatus;
  offerType: OfferType;
  price: Money;
  paymentPlan: PaymentPlan;
  depositAmount: Money | null;
  amountDue: Money;
  ticketingDeadline: string | null;
  providerConfirmationNumber: string | null;
  eTicketNumbers: string[];
  itineraryLegs: BookingFlightLeg[];
  failureReason: string | null;
}

// ---------- Payment ----------

export interface PaymentRequest {
  bookingId: string;
  paymentMethod: PaymentMethod;
  cardNumber?: string;
  cardHolderName?: string;
  expiry?: string;
  cvv?: string;
  mobileNumber?: string;
}

export interface PaymentResponse {
  paymentId: string;
  bookingId: string;
  amount: Money;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  failureReason: string | null;
}

// ---------- Ticketing ----------

export interface ETicket {
  id: string;
  bookingId: string;
  ticketNumber: string;
  providerConfirmationNumber: string | null;
  document: string;
  issuedAt: string;
}

// ---------- Auth ----------

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  phone?: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  fullName: string;
}
