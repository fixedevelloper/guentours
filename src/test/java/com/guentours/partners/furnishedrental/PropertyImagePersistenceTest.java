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
 * Regression test: Property.images used to not exist, then risked being a lazy collection.
 * Reading it outside a transaction (as PropertyResponse.from() does, in the controller, after
 * the @Transactional service method returns - open-in-view is disabled) would throw
 * "failed to lazily initialize a collection of role: Property.images - no Session" if it
 * weren't fetched eagerly.
 */
@DataJpaTest
@ActiveProfiles("test")
class PropertyImagePersistenceTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void imagesStayReadableOnADetachedProperty() {
        Property property = new Property("partner-1", "Appartement Bonanjo", PropertyType.APARTMENT,
                "Boulevard de la Liberté", "Douala", "Cameroun", 2, 1, 4,
                List.of("WIFI", "AIR_CONDITIONING"), BigDecimal.valueOf(35000), "XAF", 1, "Bel appartement");
        property.addImage("https://example.com/cover.jpg", "Vue salon", 0, true);

        String propertyId = propertyRepository.saveAndFlush(property).getId();

        entityManager.flush();
        entityManager.clear();

        Property reloaded = propertyRepository.findById(propertyId).orElseThrow();
        entityManager.detach(reloaded);

        assertThat(reloaded.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(reloaded.getImages()).hasSize(1);
        assertThat(reloaded.getImages().get(0).isPrimary()).isTrue();
    }
}
