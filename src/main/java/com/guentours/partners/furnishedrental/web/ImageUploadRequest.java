package com.guentours.partners.furnishedrental.web;

public record ImageUploadRequest(String url, String caption, Integer displayOrder, boolean isPrimary) {}
