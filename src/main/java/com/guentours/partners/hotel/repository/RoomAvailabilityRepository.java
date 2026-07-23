package com.guentours.partners.hotel.repository;

import com.guentours.partners.hotel.domain.RoomAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, String> {
    List<RoomAvailability> findByRoomTypeIdAndStayDateBetween(String roomTypeId, LocalDate from, LocalDate to);
    Optional<RoomAvailability> findByRoomTypeIdAndStayDate(String roomTypeId, LocalDate date);
}
