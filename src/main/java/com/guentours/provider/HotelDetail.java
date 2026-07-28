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
        @JsonProperty("hotel_review") HotelReviewSummary hotelReview
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
            @JsonProperty("num_reviews") Integer numReviews,
            @JsonProperty("ranking") String ranking,
            @JsonProperty("ranking_string") String rankingString,
            @JsonProperty("rate_location") Double rateLocation,
            @JsonProperty("rate_sleep") Double rateSleep,
            @JsonProperty("rate_room") Double rateRoom,
            @JsonProperty("rate_service") Double rateService,
            @JsonProperty("rate_value") Double rateValue,
            @JsonProperty("rate_cleanliness") Double rateCleanliness,
            @JsonProperty("reviews") List<Review> reviews
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(
            @JsonProperty("published_date") String publishedDate,
            @JsonProperty("rating") Integer rating,
            @JsonProperty("travel_date") String travelDate,
            @JsonProperty("title") String title,
            @JsonProperty("text") String text,
            @JsonProperty("trip_type") String tripType,
            @JsonProperty("user") User user
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            @JsonProperty("username") String username,
            @JsonProperty("user_location") UserLocation userLocation
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserLocation(
            @JsonProperty("name") String name,
            @JsonProperty("id") String id
    ) {}
}