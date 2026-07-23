package com.guentours.partners.hotel.service;

import com.guentours.partners.hotel.domain.*;
import com.guentours.partners.hotel.repository.*;
import com.guentours.partners.hotel.web.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityRepository availabilityRepository;

    public HotelService(HotelRepository hotelRepository,
                        RoomTypeRepository roomTypeRepository,
                        RoomAvailabilityRepository availabilityRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.availabilityRepository = availabilityRepository;
    }

    // --- Hôtels ---

    @Transactional
    public Hotel create(String partnerId, HotelRegistrationRequest req) {
        Hotel hotel = new Hotel(
                partnerId,
                req.name(),
                req.address(),
                req.city(),
                req.country(),
                req.starRating(),
                req.description(),
                req.coverImageUrl(), // Pris en compte
                req.amenities(),
                req.checkInTime(),
                req.checkOutTime()
        );
        return hotelRepository.save(hotel);
    }

    @Transactional(readOnly = true)
    public Page<Hotel> findByPartner(String partnerId, Pageable pageable) {
        return hotelRepository.findByPartnerId(partnerId, pageable);
    }

    @Transactional(readOnly = true)
    public Hotel findById(String id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hôtel introuvable"));
    }

    @Transactional
    public void delete(String id) {
        if (!hotelRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hôtel introuvable");
        }
        hotelRepository.deleteById(id);
    }

    @Transactional
    public void suspend(String id) {
        findById(id).suspend();
    }

    @Transactional
    public void activate(String id) {
        findById(id).activate();
    }

    // --- Galeries d'images : Hôtel ---

    @Transactional
    public Hotel addHotelImage(String hotelId, ImageUploadRequest req) {
        Hotel hotel = findById(hotelId);
        hotel.addImage(req.url(), req.caption(), req.displayOrder(), req.isPrimary());
        return hotelRepository.save(hotel);
    }

    @Transactional
    public void removeHotelImage(String hotelId, String imageId) {
        Hotel hotel = findById(hotelId);
        hotel.removeImage(imageId);
        hotelRepository.save(hotel);
    }

    // --- Types de chambres ---

    @Transactional
    public RoomType addRoomType(String hotelId, RoomTypeRequest req) {
        Hotel hotel = findById(hotelId);
        RoomType roomType = new RoomType(
                hotel,
                req.name(),
                req.maxAdults(),
                req.maxChildren(),
                req.bedType(),
                req.sizeSqm(),
                req.basePrice(),
                req.currency(),
                req.totalRooms(),
                req.coverImageUrl() // Pris en compte
        );
        return roomTypeRepository.save(roomType);
    }

    @Transactional(readOnly = true)
    public List<RoomType> getRoomTypes(String hotelId) {
        return roomTypeRepository.findByHotelId(hotelId);
    }

    @Transactional(readOnly = true)
    public RoomType findRoomTypeById(String roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type de chambre introuvable"));
    }

    // --- Galeries d'images : Types de chambres ---

    @Transactional
    public RoomType addRoomTypeImage(String roomTypeId, ImageUploadRequest req) {
        RoomType roomType = findRoomTypeById(roomTypeId);
        roomType.addImage(req.url(), req.caption(), req.displayOrder(), req.isPrimary());
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void removeRoomTypeImage(String roomTypeId, String imageId) {
        RoomType roomType = findRoomTypeById(roomTypeId);
        roomType.removeImage(imageId);
        roomTypeRepository.save(roomType);
    }

    // --- Disponibilités ---

    @Transactional
    public RoomAvailability upsertAvailability(String roomTypeId, AvailabilityUpsertRequest req) {
        RoomType roomType = findRoomTypeById(roomTypeId);

        return availabilityRepository.findByRoomTypeIdAndStayDate(roomTypeId, req.stayDate())
                .map(existing -> {
                    availabilityRepository.delete(existing);
                    return availabilityRepository.save(new RoomAvailability(roomType, req.stayDate(), req.roomsAvailable()));
                })
                .orElseGet(() -> availabilityRepository.save(new RoomAvailability(roomType, req.stayDate(), req.roomsAvailable())));
    }

    @Transactional(readOnly = true)
    public List<RoomAvailability> getAvailability(String roomTypeId, java.time.LocalDate from, java.time.LocalDate to) {
        return availabilityRepository.findByRoomTypeIdAndStayDateBetween(roomTypeId, from, to);
    }
}