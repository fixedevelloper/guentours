package com.guentours.geo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelCityRepository extends JpaRepository<HotelCity, Long> {

    Optional<HotelCity> findByCityNameIgnoreCaseAndCountryNameIgnoreCase(String cityName, String countryName);

    @Query("""
            select c from HotelCity c
            where upper(c.cityName) like upper(concat(:query, '%'))
               or upper(c.countryName) like upper(concat('%', :query, '%'))
            order by c.cityName
            """)
    List<HotelCity> search(@Param("query") String query, Pageable pageable);
}
