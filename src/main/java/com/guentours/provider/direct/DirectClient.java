package com.guentours.provider.direct;

import com.guentours.partners.carrental.domain.Vehicle;
import com.guentours.partners.carrental.domain.VehicleAvailability;
import com.guentours.partners.carrental.repository.VehicleAvailabilityRepository;
import com.guentours.partners.carrental.repository.VehicleRepository;
import com.guentours.partners.flight.domain.AirlineFlight;
import com.guentours.partners.flight.domain.DepartureStatus;
import com.guentours.partners.flight.domain.FlightAvailability;
import com.guentours.partners.flight.domain.FlightFare;
import com.guentours.partners.flight.repository.AirlineFlightRepository;
import com.guentours.partners.flight.repository.FlightAvailabilityRepository;
import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import com.guentours.partners.furnishedrental.repository.PropertyAvailabilityRepository;
import com.guentours.partners.furnishedrental.repository.PropertyRepository;
import com.guentours.partners.hotel.domain.Hotel;
import com.guentours.partners.hotel.domain.ListingStatus;
import com.guentours.partners.hotel.domain.RoomAvailability;
import com.guentours.partners.hotel.domain.RoomType;
import com.guentours.partners.hotel.repository.HotelRepository;
import com.guentours.partners.hotel.repository.RoomAvailabilityRepository;
import com.guentours.partners.hotel.repository.RoomTypeRepository;
import com.guentours.provider.*;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.provider.dto.PropertyPriceVerification;
import com.guentours.provider.dto.VehiclePriceVerification;
import com.guentours.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DirectClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(DirectClient.class);
    private final AirlineFlightRepository airlineFlightRepository;
    private final FlightAvailabilityRepository flightAvailabilityRepository;
    private final HotelRepository hotelRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityRepository vehicleAvailabilityRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyAvailabilityRepository propertyAvailabilityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    public DirectClient(AirlineFlightRepository airlineFlightRepository, FlightAvailabilityRepository flightAvailabilityRepository, HotelRepository hotelRepository,
                        VehicleRepository vehicleRepository, VehicleAvailabilityRepository vehicleAvailabilityRepository,
                        PropertyRepository propertyRepository, PropertyAvailabilityRepository propertyAvailabilityRepository, RoomTypeRepository roomTypeRepository, RoomAvailabilityRepository roomAvailabilityRepository) {
        this.airlineFlightRepository = airlineFlightRepository;
        this.flightAvailabilityRepository = flightAvailabilityRepository;
        this.hotelRepository = hotelRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleAvailabilityRepository = vehicleAvailabilityRepository;
        this.propertyRepository = propertyRepository;
        this.propertyAvailabilityRepository = propertyAvailabilityRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomAvailabilityRepository = roomAvailabilityRepository;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.DIRECT;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<FlightOffer> searchFlights(FlightSearchCriteria criteria) {
        List<FlightAvailability> availabilities =
                flightAvailabilityRepository.findByFare_Flight_OriginAirportCodeAndFare_Flight_DestinationAirportCode(
                        criteria.origin(),
                        criteria.destination()
                );

        return availabilities.stream()
                .filter(av -> matchesCabinClass(av, criteria))
                .filter(av -> av.getStatus() == DepartureStatus.SCHEDULED)
                .map(av -> toOffer(av, criteria))
                .toList();
    }



    /** Prix total (toutes nuits, toutes chambres demandées) du type de chambre le moins cher encore disponible. */
    private record PricedRoom(String roomTypeName, Money totalPrice) {}

    @Override
    public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
        List<Hotel> hotels = hotelRepository.findByCityIgnoreCaseAndCountryIgnoreCaseAndStatus(
                criteria.cityCode(),
                "CM",
                ListingStatus.ACTIVE
        );

        return hotels.stream()
                .filter(hotel -> matchesHotelCriteria(hotel, criteria))
                .map(hotel -> cheapestAvailableRoom(hotel, criteria)
                        .map(priced -> toOffer(hotel, priced.totalPrice(), priced.roomTypeName(), criteria))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Trouve le type de chambre le moins cher de l'hôtel qui reste disponible pour toutes les
     * nuits du séjour, en nombre suffisant. Renvoie {@code empty()} si aucun type de chambre
     * n'a assez de stock sur une des nuits demandées.
     */
    private Optional<PricedRoom> cheapestAvailableRoom(Hotel hotel, HotelSearchCriteria criteria) {
        List<RoomType> roomTypes = roomTypeRepository.findByHotelId(hotel.getId());
        List<LocalDate> nights = criteria.checkIn().datesUntil(criteria.checkOut()).toList();
        if (nights.isEmpty()) {
            return Optional.empty();
        }

        int roomsNeeded = Math.max(criteria.rooms(), 1);

        return roomTypes.stream()
                .filter(rt -> fitsOccupancy(rt, criteria))
                .map(rt -> priceIfAvailable(rt, nights, roomsNeeded))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparing(p -> p.totalPrice().amount()));
    }

    private boolean fitsOccupancy(RoomType roomType, HotelSearchCriteria criteria) {
        int adults = criteria.adults();
        int children = criteria.adults(); // ajuste si HotelSearchCriteria n'a pas ce champ tel quel
        return roomType.getMaxAdults() >= adults && roomType.getMaxChildren() >= children;
    }

    /**
     * Calcule le prix total du séjour pour ce type de chambre si le stock est suffisant chaque
     * nuit ; renvoie {@code empty()} dès qu'une nuit n'a pas assez de chambres disponibles.
     * Une nuit sans entrée {@code RoomAvailability} explicite retombe sur le stock/prix par
     * défaut du type de chambre (totalRooms / basePrice) plutôt que d'être considérée indisponible.
     */
    private Optional<PricedRoom> priceIfAvailable(RoomType roomType, List<LocalDate> nights, int roomsNeeded) {
        Map<LocalDate, RoomAvailability> overridesByDate = roomAvailabilityRepository
                .findByRoomTypeIdAndStayDateBetween(roomType.getId(), nights.get(0), nights.get(nights.size() - 1))
                .stream()
                .collect(Collectors.toMap(RoomAvailability::getStayDate, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate night : nights) {
            RoomAvailability override = overridesByDate.get(night);
            int roomsAvailable = override != null ? override.getRoomsAvailable() : roomType.getTotalRooms();

            if (roomsAvailable < roomsNeeded) {
                return Optional.empty();
            }

            BigDecimal nightlyPrice = override != null && override.getPriceOverride() != null
                    ? override.getPriceOverride()
                    : roomType.getBasePrice();

            total = total.add(nightlyPrice.multiply(BigDecimal.valueOf(roomsNeeded)));
        }

        return Optional.of(new PricedRoom(roomType.getName(), new Money(total, roomType.getCurrency())));
    }

    @Override
    public List<VehicleOffer> searchVehicles(VehicleSearchCriteria criteria) {
        try {
            // Filtrage réel : uniquement sur pickupCity/dates/category, comme avant.
            // dropoffCity/heures/withDriver/driverAge25Plus sont acceptés et reportés sur l'offre
            // pour affichage, mais n'affectent NI le filtrage NI le prix (aucune règle tarifaire
            // confirmée pour ces options à ce stade).
            List<Vehicle> vehicles = vehicleRepository.findActiveByPickupCity(criteria.pickupCity());

            return vehicles.stream()
                   // .filter(v -> criteria.category() == null || v.getCategory().name().equalsIgnoreCase(criteria.category()))
                  //  .filter(v -> isAvailableForRange(v, criteria.rentalStart(), criteria.rentalEnd()))
                    .map(v -> toOffer(v, criteria))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur recherche véhicules DIRECT pour {}", criteria, e);
            return List.of();
        }
    }

    private VehicleOffer toOffer(Vehicle vehicle, VehicleSearchCriteria criteria) {
        long days = ChronoUnit.DAYS.between(criteria.rentalStart(), criteria.rentalEnd());
        days = Math.max(days, 1);

        BigDecimal total = vehicle.getPricePerDay().multiply(BigDecimal.valueOf(days));
        String dropoffCity = criteria.dropoffCity() != null ? criteria.dropoffCity() : criteria.pickupCity();

        return new VehicleOffer(
                ProviderType.DIRECT,
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getCategory().name(),
                vehicle.getTransmission().name(),
                vehicle.getSeats(),
                Boolean.TRUE.equals(vehicle.getAirConditioning()),
                criteria.pickupCity(),
                dropoffCity,
                criteria.rentalStart(),
                criteria.pickupTime(),
                criteria.rentalEnd(),
                criteria.dropoffTime(),
                criteria.withDriver(),
                criteria.driverAge25Plus(),
                new Money(vehicle.getPricePerDay(), vehicle.getCurrency()),
                new Money(total, vehicle.getCurrency()),
                Map.of()
        );
    }

    @Override
    public List<PropertyOffer> searchProperties(PropertySearchCriteria criteria) {
        try {
            List<Property> properties = propertyRepository.findActiveByCityAndCapacity(criteria.city(), criteria.guests());


            return properties.stream()
                    .filter(p -> criteria.propertyType() == null || p.getPropertyType().name().equalsIgnoreCase(criteria.propertyType()))
                    .filter(p -> criteria.bedrooms() == null || p.getBedrooms() >= criteria.bedrooms())
                    //.filter(p -> isAvailableForRange(p, criteria.checkIn(), criteria.checkOut()))
                    .map(p -> toOffer(p, criteria))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur recherche logements DIRECT pour {}", criteria, e);
            return List.of();
        }
    }

    private PropertyOffer toOffer(Property property, PropertySearchCriteria criteria) {
        long nights = ChronoUnit.DAYS.between(criteria.checkIn(), criteria.checkOut());
        nights = Math.max(nights, 1);

        BigDecimal total = property.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        return new PropertyOffer(
                ProviderType.DIRECT,
                property.getId(),
                property.getTitle(),
                property.getPropertyType().name(),
                property.getCity(),
                property.getCountry(),
                property.getBedrooms(),
                property.getMaxGuests(),
                criteria.entirePlace(),
                criteria.checkIn(),
                criteria.checkOut(),
                new Money(property.getPricePerNight(), property.getCurrency()),
                new Money(total, property.getCurrency()),
                Map.of()
        );
    }
    /**
     * ⚠️ Simplification assumée : un véhicule est considéré disponible sur toute la période
     * uniquement si CHAQUE jour de la plage a une entrée VehicleAvailability avec unitsAvailable > 0.
     * Si aucune entrée n'existe pour un jour donné, le véhicule est considéré INDISPONIBLE ce jour-là
     * (pas de fallback sur Vehicle.unitsCount) — à confirmer que c'est le comportement voulu, sinon
     * il faut un fallback explicite sur l'inventaire de base.
     */
    private boolean isAvailableForRange(Vehicle vehicle, LocalDate start, LocalDate end) {
        List<VehicleAvailability> availabilities =
                vehicleAvailabilityRepository.findByVehicle_IdAndRentDateBetween(vehicle.getId(), start, end);

        long expectedDays = start.until(end).getDays() + 1;
        if (availabilities.size() < expectedDays) {
            return false; // jour(s) manquant(s) dans le calendrier de dispo
        }
        return availabilities.stream().allMatch(a -> a.getUnitsAvailable() != null && a.getUnitsAvailable() > 0);
    }

    /** Même remarque que isAvailableForRange : aucune entrée pour un jour donné = indisponible. */
    private boolean isAvailableForRange(Property property, LocalDate checkIn, LocalDate checkOut) {
        List<PropertyAvailability> availabilities =
                propertyAvailabilityRepository.findByProperty_IdAndStayDateBetween(property.getId(), checkIn, checkOut);

        long expectedNights = checkIn.until(checkOut).getDays();
        if (availabilities.size() < expectedNights) {
            return false;
        }
        return availabilities.stream().allMatch(a -> Boolean.TRUE.equals(a.getIsAvailable()));
    }




    private FlightOffer toOffer(FlightAvailability flightAvailability, FlightSearchCriteria criteria) {
        FlightFare fare = flightAvailability.getFare();
        AirlineFlight flight = fare.getFlight();

        LocalDateTime departureDateTime = flightAvailability.getFlightDate().atTime(flight.getDepartureTime());
        LocalDateTime arrivalDateTime = flightAvailability.getFlightDate().atTime(flight.getArrivalTime());

        BigDecimal price = flightAvailability.getPriceOverride() != null
                ? flightAvailability.getPriceOverride()
                : fare.getBasePrice();

        return new FlightOffer(
                ProviderType.DIRECT,
                flightAvailability.getId(),
                flight.getPartnerId(),
                flight.getFlightNumber(),
                flight.getOriginAirportCode(),
                flight.getDestinationAirportCode(),
                departureDateTime,
                arrivalDateTime,
                fare.getCabinClass().name(),
                new Money(price, fare.getCurrency()),
                flightAvailability.getSeatsAvailable(),
                Map.of(
                        "flightAvailabilityId", flightAvailability.getId(),
                        "fareId", fare.getId(),
                        "flightId", flight.getId()
                )
        );
    }
    private HotelOffer toOffer(Hotel hotel, HotelSearchCriteria criteria, Money price) {
        return new HotelOffer(
                ProviderType.DIRECT,
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                "STANDARD",
                criteria.checkIn(),
                criteria.checkOut(),
                price,
                hotel.getStarRating() != null ? hotel.getStarRating() : 0.0
        );
    }
    private HotelOffer toOffer(Hotel hotel, Money price, String roomType,HotelSearchCriteria criteria) {


        return new HotelOffer(
                ProviderType.DIRECT,
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                roomType,
                criteria.checkIn(),
                criteria.checkOut(),
                price,
                hotel.getStarRating() != null ? hotel.getStarRating().doubleValue() : 0.0,
                Map.of(
                        "hotelId", hotel.getId(),
                        "partnerId", hotel.getPartnerId(),
                        "country", hotel.getCountry()
                )
        );
    }

    private boolean matchesHotelCriteria(Hotel hotel, HotelSearchCriteria criteria) {
 /*       if (criteria.starRating() != null && hotel.getStarRating() != null) {
            return hotel.getStarRating() >= criteria.starRating();
        }*/
        return true;
    }
    private boolean matchesCabinClass(FlightAvailability av, FlightSearchCriteria criteria) {
        return av.getFare().getCabinClass().name().equals(criteria.cabinClass());
    }
    // --- Méthodes vol/hôtel restantes, inchangées tant qu'on n'a pas les entités correspondantes ---
    @Override
    public ProviderBookingConfirmation createVehicleHold(VehicleBookingRequest request) {
        VehicleOffer offer = request.offer();
        try {
            List<VehicleAvailability> availabilities = vehicleAvailabilityRepository
                    .findByVehicle_IdAndRentDateBetween(offer.providerOfferId(), offer.rentalStart(), offer.rentalEnd());

            long expectedDays = ChronoUnit.DAYS.between(offer.rentalStart(), offer.rentalEnd()) + 1;
            if (availabilities.size() < expectedDays
                    || availabilities.stream().anyMatch(a -> a.getUnitsAvailable() == null || a.getUnitsAvailable() < 1)) {
                return new ProviderBookingConfirmation(offer.providerType(), "", null,false);
            }

            availabilities.forEach(a -> a.decrementUnits(1));
            vehicleAvailabilityRepository.saveAll(availabilities);

            String bookingRef = "DIRECT-VEH-" + UUID.randomUUID();
            // Pas de deadline de ticketing réelle pour un véhicule DIRECT : hold = confirmation immédiate.
            return new ProviderBookingConfirmation(offer.providerType(), bookingRef, LocalDateTime.now().plusMinutes(30),true);
        } catch (Exception e) {
            log.error("Erreur lors du hold véhicule pour l'offre {}", offer.providerOfferId(), e);
            return new ProviderBookingConfirmation(offer.providerType(), "", null,false);
        }
    }

    @Override
    public ProviderBookingConfirmation createPropertyHold(PropertyBookingRequest request) {
        PropertyOffer offer = request.offer();
        try {
            List<PropertyAvailability> availabilities = propertyAvailabilityRepository
                    .findByProperty_IdAndStayDateBetween(offer.providerOfferId(), offer.checkIn(), offer.checkOut());

            long expectedNights = ChronoUnit.DAYS.between(offer.checkIn(), offer.checkOut());
            if (availabilities.size() < expectedNights
                    || availabilities.stream().anyMatch(a -> !Boolean.TRUE.equals(a.getIsAvailable()))) {
                return new ProviderBookingConfirmation(offer.providerType(), "", null,false);
            }

            // ⚠️ PropertyAvailability n'a pas de setter isAvailable(false) visible dans l'entité fournie —
            // il faudra en ajouter un pour marquer les nuits comme réservées, sinon deux clients pourraient
            // réserver le même logement sur les mêmes dates en concurrence.
            String bookingRef = "DIRECT-PROP-" + UUID.randomUUID();
            return new ProviderBookingConfirmation(offer.providerType(), bookingRef, LocalDateTime.now().plusMinutes(30),true);
        } catch (Exception e) {
            log.error("Erreur lors du hold logement pour l'offre {}", offer.providerOfferId(), e);
            return new ProviderBookingConfirmation(offer.providerType(), "", null,false);
        }
    }

    @Override
    public void cancelVehicleBooking(String bookingRef) {
        // ⚠️ Pas de table de correspondance bookingRef -> unités décrémentées : impossible de
        // restaurer l'inventaire exact sans un enregistrement de réservation dédié (ex: une entité
        // VehicleBookingHold liant bookingRef aux VehicleAvailability décrémentées).
        log.warn("cancelVehicleBooking non implémenté : impossible de restaurer l'inventaire pour {}", bookingRef);
    }

    @Override
    public void cancelPropertyBooking(String bookingRef) {
        log.warn("cancelPropertyBooking non implémenté : impossible de restaurer l'inventaire pour {}", bookingRef);
    }
    @Override
    public VehiclePriceVerification verifyVehiclePrice(VehicleOffer offer) {
        try {
            boolean available = isAvailableForRange(
                    vehicleRepository.findById(offer.providerOfferId()).orElseThrow(),
                    offer.rentalStart(), offer.rentalEnd());
            // DIRECT n'a pas de fluctuation de prix externe : le tarif vient de notre propre inventaire,
            // donc currentPrice == offer.totalPrice() par construction tant que le véhicule existe encore.
            return new VehiclePriceVerification(offer.totalPrice(), available);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification prix véhicule pour l'offre {}", offer.providerOfferId(), e);
            return new VehiclePriceVerification(offer.totalPrice(), false);
        }
    }

    @Override
    public PropertyPriceVerification verifyPropertyPrice(PropertyOffer offer) {
        try {
            boolean available = isAvailableForRange(
                    propertyRepository.findById(offer.providerOfferId()).orElseThrow(),
                    offer.checkIn(), offer.checkOut());
            return new PropertyPriceVerification(offer.totalPrice(), available);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification prix logement pour l'offre {}", offer.providerOfferId(), e);
            return new PropertyPriceVerification(offer.totalPrice(), false);
        }
    }
    @Override
    public FlightPriceVerification verifyFlightPrice(FlightOffer offer) { return null; }
    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer) { return null; }
    @Override
    public HotelDetail getDetailHotel(HotelOffer offer) { return null; }
    @Override
    public List<RoomOffer> getRoomOffers(HotelOffer offer) { return List.of(); }
    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) { return null; }
    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) { return null; }
    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) { return null; }
    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) { return null; }
    @Override
    public void cancelFlightBooking(String pnrCode) {}
    @Override
    public void cancelHotelBooking(String hotelBookingRef) {}
}