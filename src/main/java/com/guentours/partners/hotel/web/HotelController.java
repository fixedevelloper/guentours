package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.RoomAvailability;
import com.guentours.partners.hotel.domain.RoomType;
import com.guentours.partners.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/partners/{partnerId}/hotels")
//@PreAuthorize("hasRole('PARTNER_HOTEL') and #partnerId == authentication.principal.partnerId")
public class HotelController {
    private final HotelService service;
    public HotelController(HotelService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse create(@PathVariable String partnerId, @Valid @RequestBody HotelRegistrationRequest req) {
        return HotelResponse.from(service.create(partnerId, req));
    }

    @GetMapping
    public Page<HotelResponse> list(@PathVariable String partnerId, Pageable pageable) {
        return service.findByPartner(partnerId, pageable).map(HotelResponse::from);
    }

    @GetMapping("/{hotelId}")
    public HotelResponse getOne(@PathVariable String hotelId) {
        return HotelResponse.from(service.findById(hotelId));
    }

    @DeleteMapping("/{hotelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String hotelId) { service.delete(hotelId); }

    @PatchMapping("/{hotelId}/suspend")
    public void suspend(@PathVariable String hotelId) { service.suspend(hotelId); }

    @PatchMapping("/{hotelId}/activate")
    public void activate(@PathVariable String hotelId) { service.activate(hotelId); }

    @PostMapping("/{hotelId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse addHotelImage(
            @PathVariable String hotelId,
            @Valid @RequestBody ImageUploadRequest req) {
        return HotelResponse.from(service.addHotelImage(hotelId, req));
    }

    @PostMapping("/{hotelId}/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse addRoomType(@PathVariable String hotelId, @Valid @RequestBody RoomTypeRequest req) {
        return RoomTypeResponse.from(service.addRoomType(hotelId, req));
    }

    @GetMapping("/{hotelId}/room-types")
    public List<RoomTypeResponse> getRoomTypes(@PathVariable String hotelId) {
        return service.getRoomTypes(hotelId).stream()
                .map(RoomTypeResponse::from)
                .toList();
    }

    @GetMapping("/room-types/{roomTypeId}")
    public RoomTypeResponse getRoomType(@PathVariable String roomTypeId) {
        return RoomTypeResponse.from(service.findRoomTypeById(roomTypeId));
    }

    @PostMapping("/room-types/{roomTypeId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse addRoomTypeImage(@PathVariable String roomTypeId, @Valid @RequestBody ImageUploadRequest req) {
        return RoomTypeResponse.from(service.addRoomTypeImage(roomTypeId, req));
    }
    @PutMapping("/room-types/{roomTypeId}/availability")
    public RoomAvailabilityResponse upsertAvailability(@PathVariable String roomTypeId,
                                                       @Valid @RequestBody AvailabilityUpsertRequest req) {
        return RoomAvailabilityResponse.from(service.upsertAvailability(roomTypeId, req));
    }

    @GetMapping("/room-types/{roomTypeId}/availability")
    public List<RoomAvailabilityResponse> getAvailability(@PathVariable String roomTypeId,
                                                          @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.getAvailability(roomTypeId, from, to).stream().map(RoomAvailabilityResponse::from).toList();
    }
}