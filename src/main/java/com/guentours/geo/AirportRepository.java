package com.guentours.geo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AirportRepository extends JpaRepository<Airport, String> {

    @Query("""
            select a from Airport a
            where upper(a.airportCode) like upper(concat(:query, '%'))
               or upper(a.city) like upper(concat('%', :query, '%'))
               or upper(a.airportName) like upper(concat('%', :query, '%'))
            order by a.airportCode
            """)
    List<Airport> search(@Param("query") String query, Pageable pageable);
}
