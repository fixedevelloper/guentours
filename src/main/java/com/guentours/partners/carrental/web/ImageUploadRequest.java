package com.guentours.partners.carrental.web;

public record ImageUploadRequest(String url, String caption, Integer displayOrder, boolean isPrimary) {}
