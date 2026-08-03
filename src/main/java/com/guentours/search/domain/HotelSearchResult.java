package com.guentours.search.domain;

import java.util.List;

/**
 * {@code searchId} is {@code null} when no enabled provider captured a pagination token for this
 * particular search (e.g. Travelport was disabled, or returned no offers to capture one from) -
 * the frontend should simply hide its "load more" action in that case, since
 * {@code HotelSearchService#loadMore} would have nothing to resume.
 */
public record HotelSearchResult(String searchId, List<HarmonizedHotelOffer> offers) {
}
