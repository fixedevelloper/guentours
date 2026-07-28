package com.guentours.partners.hotel;

import com.guentours.partners.hotel.domain.Hotel;
import com.guentours.partners.hotel.domain.RoomType;
import com.guentours.partners.hotel.repository.HotelRepository;
import com.guentours.partners.hotel.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: HotelImage/RoomTypeImage used to be mapped as a unidirectional
 * @OneToMany with a NOT NULL @JoinColumn, which makes Hibernate INSERT the row
 * without the foreign key (then UPDATE it) - failing immediately since hotel_id/
 * room_type_id has no default. The image side must own the relationship instead.
 */
@DataJpaTest
@ActiveProfiles("test")
class HotelImagePersistenceTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void addingAnImageToAHotelPersistsWithoutViolatingTheNotNullForeignKey() {
        Hotel hotel = new Hotel("partner-1", "Le Palace", "1 rue des Fleurs", "Douala", "Cameroun",
                4, "Un bel hôtel", null, null, null, null);
        hotel.addImage("https://example.com/facade.jpg", "Façade", 0, true);

        Hotel saved = hotelRepository.saveAndFlush(hotel);

        assertThat(saved.getImages()).hasSize(1);
        assertThat(saved.getImages().get(0).getHotel().getId()).isEqualTo(saved.getId());
        assertThat(saved.getCoverImageUrl()).isEqualTo("https://example.com/facade.jpg");
        String hotelId = saved.getId();

        // Simule HotelResponse.from() lu hors transaction (open-in-view=false) : recharge une
        // instance fraîche, la détache AVANT tout accès à la collection, puis la lit - un fetch
        // lazy lèverait LazyInitializationException ici puisque la session n'a jamais initialisé
        // le proxy pendant qu'elle était encore ouverte.
        entityManager.flush();
        entityManager.clear();
        Hotel reloaded = hotelRepository.findById(hotelId).orElseThrow();
        entityManager.detach(reloaded);
        assertThat(reloaded.getImages()).hasSize(1);
    }

    @Test
    void addingAnImageToARoomTypePersistsWithoutViolatingTheNotNullForeignKey() {
        Hotel hotel = new Hotel("partner-1", "Le Palace", "1 rue des Fleurs", "Douala", "Cameroun",
                4, "Un bel hôtel", null, null, null, null);
        hotelRepository.saveAndFlush(hotel);

        RoomType roomType = new RoomType(hotel, "Suite Deluxe", 2, 1, "King", 35.0,
                BigDecimal.valueOf(50000), "XAF", 5, null);
        roomType.addImage("https://example.com/suite.jpg", "Suite", 0, true);

        RoomType saved = roomTypeRepository.saveAndFlush(roomType);

        assertThat(saved.getImages()).hasSize(1);
        assertThat(saved.getImages().get(0).getRoomType().getId()).isEqualTo(saved.getId());
        assertThat(saved.getCoverImageUrl()).isEqualTo("https://example.com/suite.jpg");
        String roomTypeId = saved.getId();

        // Cf. le même scénario que pour Hotel ci-dessus : RoomTypeResponse.from() lit cette
        // collection hors transaction.
        entityManager.flush();
        entityManager.clear();
        RoomType reloaded = roomTypeRepository.findById(roomTypeId).orElseThrow();
        entityManager.detach(reloaded);
        assertThat(reloaded.getImages()).hasSize(1);
    }
}
