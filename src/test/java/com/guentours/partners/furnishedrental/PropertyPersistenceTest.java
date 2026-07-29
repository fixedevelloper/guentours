package com.guentours.partners.furnishedrental;

import com.guentours.partners.furnishedrental.domain.Property;
import com.guentours.partners.furnishedrental.domain.PropertyType;
import com.guentours.partners.furnishedrental.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: Property.amenities used to be a lazy @ElementCollection. Reading it
 * outside a transaction (as PropertyResponse.from() does, in the controller, after the
 * @Transactional service method returns - open-in-view is disabled) threw:
 * "failed to lazily initialize a collection of role: Property.amenities - no Session".
 */
@DataJpaTest
@ActiveProfiles("test")
class PropertyPersistenceTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void amenitiesStayReadableOnADetachedProperty() {
        Property property = new Property("partner-1", "Appartement Bonanjo", PropertyType.APARTMENT,
                "Boulevard de la Liberté", "Douala", "Cameroun", 2, 1, 4,
                List.of("WIFI", "AIR_CONDITIONING"), BigDecimal.valueOf(35000), "XAF", 1, "Bel appartement");

        String propertyId = propertyRepository.saveAndFlush(property).getId();

        entityManager.flush();
        entityManager.clear();

        Property reloaded = propertyRepository.findById(propertyId).orElseThrow();
        entityManager.detach(reloaded);

        assertThat(reloaded.getAmenities()).containsExactlyInAnyOrder("WIFI", "AIR_CONDITIONING");
    }
}
