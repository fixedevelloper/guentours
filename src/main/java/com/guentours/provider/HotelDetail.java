package com.guentours.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelDetail(
        @JsonProperty("hotelId") String hotelId,
        @JsonProperty("name") String name,
        @JsonProperty("address") String address,
        @JsonProperty("city") String city,
        @JsonProperty("country") String country,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("postalCode") String postalCode,
        @JsonProperty("latitude") Double latitude,
        @JsonProperty("longitude") Double longitude,
        @JsonProperty("hotelRating") Double hotelRating,
        @JsonProperty("description") HotelDescription description,
        @JsonProperty("facilities") List<String> facilities,
        @JsonProperty("hotelImages") List<HotelImage> hotelImages,
        @JsonProperty("hotelReview") HotelReviewSummary hotelReview
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelDescription(
            @JsonProperty("content") String content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelImage(
            @JsonProperty("caption") String caption,
            @JsonProperty("url") String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelReviewSummary(
            @JsonProperty("rating") Double rating,
            @JsonProperty("numReviews") Integer numReviews,
            @JsonProperty("ranking") String ranking,
            @JsonProperty("rankingString") String rankingString,
            @JsonProperty("rateLocation") Double rateLocation,
            @JsonProperty("rateSleep") Double rateSleep,
            @JsonProperty("rateRoom") Double rateRoom,
            @JsonProperty("rateService") Double rateService,
            @JsonProperty("rateValue") Double rateValue,
            @JsonProperty("rateCleanliness") Double rateCleanliness,
            @JsonProperty("reviews") List<Review> reviews
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(
            @JsonProperty("publishedDate") String publishedDate,
            @JsonProperty("rating") Integer rating,
            @JsonProperty("travelDate") String travelDate,
            @JsonProperty("title") String title,
            @JsonProperty("text") String text,
            @JsonProperty("tripType") String tripType,
            @JsonProperty("user") User user
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            @JsonProperty("username") String username,
            @JsonProperty("userLocation") UserLocation userLocation
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserLocation(
            @JsonProperty("name") String name,
            @JsonProperty("id") String id
    ) {}
}