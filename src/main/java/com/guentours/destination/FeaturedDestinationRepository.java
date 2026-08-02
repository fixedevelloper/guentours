package com.guentours.destination;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeaturedDestinationRepository extends JpaRepository<FeaturedDestination, String> {

    List<FeaturedDestination> findByActiveTrueOrderByDisplayOrderAsc();

    List<FeaturedDestination> findAllByOrderByDisplayOrderAsc();

    Optional<FeaturedDestination> findByDestinationCodeIgnoreCase(String destinationCode);

    boolean existsByDestinationCodeIgnoreCaseAndIdNot(String destinationCode, String id);
}
