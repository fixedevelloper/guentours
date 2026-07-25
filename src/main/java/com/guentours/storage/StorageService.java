package com.guentours.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** Uploads a file under the given key prefix and returns its publicly accessible URL. */
    String upload(String keyPrefix, MultipartFile file);

    void delete(String publicUrl);
}