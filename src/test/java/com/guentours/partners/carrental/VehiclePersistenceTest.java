package com.guentours.partners.carrental;

import com.guentours.partners.carrental.domain.Transmission;
import com.guentours.partners.carrental.domain.Vehicle;
import com.guentours.partners.carrental.domain.VehicleCategory;
import com.guentours.partners.carrental.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: Vehicle.pickupLocations used to be a lazy @ElementCollection. Reading it
 * outside a transaction (as VehicleResponse.from() does, in the controller, after the
 * @Transactional service method returns - open-in-view is disabled) threw:
 * "failed to lazily initialize a collection of role: Vehicle.pickupLocations - no Session".
 */
@DataJpaTest
@ActiveProfiles("test")
class VehiclePersistenceTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void pickupLocationsStayReadableOnADetachedVehicle() {
        Vehicle vehicle = new Vehicle("partner-1", "Toyota", "Corolla", 2022,
                VehicleCategory.ECONOMY, Transmission.MANUAL, 5, true,
                BigDecimal.valueOf(25000), "XAF", 3, List.of("Douala", "Yaounde"));

        String vehicleId = vehicleRepository.saveAndFlush(vehicle).getId();

        entityManager.flush();
        entityManager.clear();

        Vehicle reloaded = vehicleRepository.findById(vehicleId).orElseThrow();
        entityManager.detach(reloaded);

        assertThat(reloaded.getPickupLocations()).containsExactlyInAnyOrder("Douala", "Yaounde");
    }

    @Test
    void imagesStayReadableOnADetachedVehicle() {
        Vehicle vehicle = new Vehicle("partner-1", "Toyota", "Corolla", 2022,
                VehicleCategory.ECONOMY, Transmission.MANUAL, 5, true,
                BigDecimal.valueOf(25000), "XAF", 3, List.of("Douala", "Yaounde"));
        vehicle.addImage("https://example.com/cover.jpg", "Vue avant", 0, true);

        String vehicleId = vehicleRepository.saveAndFlush(vehicle).getId();

        entityManager.flush();
        entityManager.clear();

        Vehicle reloaded = vehicleRepository.findById(vehicleId).orElseThrow();
        entityManager.detach(reloaded);

        assertThat(reloaded.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(reloaded.getImages()).hasSize(1);
        assertThat(reloaded.getImages().get(0).isPrimary()).isTrue();
    }
}
