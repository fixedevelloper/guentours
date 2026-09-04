package com.guentours.booking.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    /**
     * Tags a booking with the reseller that sold it - a single-column bulk update, deliberately
     * not a load-mutate-{@link #save} round trip. The booking this targets was just created by
     * {@code BookingService#checkout}, whose provider hold ({@code completeHold}) keeps running
     * concurrently on its own async thread/transaction and will itself save the same row soon
     * after; loading the entity here and saving it back would race that write under
     * {@code Booking}'s {@code @Version} optimistic lock and 500 whichever save loses. A bulk
     * update isn't versioned, so it can't collide with it. {@code clearAutomatically} is
     * defensive: a caller that (unlike today's {@code ResellerBookingService}, which deliberately
     * runs this outside any ambient transaction - see its own Javadoc) still holds the booking
     * entity as managed would otherwise have Hibernate's dirty checking flush a second, versioned
     * write for the very field this bulk update just set, at commit - reintroducing the race.
     * {@code @Transactional}: unlike the base CRUD methods {@link JpaRepository} itself provides,
     * a custom {@code @Modifying} query method isn't transactional by default - needs its own
     * transaction regardless of whether the caller has one (today's caller deliberately doesn't,
     * see {@code ResellerBookingService}), else Hibernate refuses to run the update at all.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Booking b set b.resellerId = :resellerId where b.id = :id")
    void assignReseller(@Param("id") String id, @Param("resellerId") String resellerId);

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

    /** Confirmed flight bookings still missing e-ticket numbers - candidates for
     *  {@code ETicketReconciliationJob} (some providers ticket asynchronously; see
     *  TravelProviderClient#checkForIssuedTickets). */
    @Query("""
            select b from Booking b
            where b.status = com.guentours.booking.domain.BookingStatus.CONFIRMED
            and b.offerType = com.guentours.booking.domain.OfferType.FLIGHT
            and b.eTicketNumbers is empty
            """)
    List<Booking> findConfirmedFlightsMissingETickets();
}
