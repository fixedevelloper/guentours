package com.guentours.partners.hotel.repository;

import com.guentours.partners.hotel.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
    List<RoomType> findByHotelId(String hotelId);
}
