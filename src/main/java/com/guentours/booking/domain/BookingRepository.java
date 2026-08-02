package com.guentours.booking.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    /** Top flight destinations by booking count - powers the homepage's featured-destinations
     *  auto-suggestion (see {@code com.guentours.destination.FeaturedDestinationService}). */
    @Query("""
            select b.destination as destinationCode, count(b) as bookingCount
            from Booking b
            where b.offerType = com.guentours.booking.domain.OfferType.FLIGHT and b.destination is not null
            group by b.destination
            order by count(b) desc
            """)
    List<DestinationBookingCount> countFlightBookingsByDestination(Pageable pageable);

    /** Holds whose provider deadline has lapsed without being fully paid - candidates for auto-cancellation. */
    List<Booking> findByStatusInAndTicketingDeadlineBefore(List<BookingStatus> statuses, LocalDateTime cutoff);
    Page<Booking> findByPartnerId(String partnerId, Pageable pageable);
    Page<Booking> findByResellerId(String resellerId, Pageable pageable);
    /**
     * Recherche les réservations PAY_LATER non finalisées créées avant un instant donné.
     */
    List<Booking> findByPaymentPlanAndStatusInAndCreatedAtBefore(
            PaymentPlan paymentPlan,
            List<BookingStatus> statuses,
            Instant expirationThreshold
    );
}
