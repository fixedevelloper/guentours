package com.guentours.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** Uploads a file under the given key prefix and returns its publicly accessible URL. */
    String upload(String keyPrefix, MultipartFile file);

    /**
     * Uploads server-generated content (e.g. a rendered PDF) that never went through a
     * {@link MultipartFile} - unlike {@link #upload(String, MultipartFile)}, not restricted to
     * images/5MB, and the object key is {@code keyPrefix + "/" + filename} verbatim rather than a
     * random UUID, so re-uploading under the same prefix/filename overwrites the same object
     * instead of accumulating duplicates.
     */
    String upload(String keyPrefix, byte[] content, String contentType, String filename);

    /** Downloads a previously uploaded object back into memory, e.g. to attach it to an email. */
    byte[] download(String publicUrl);

    void delete(String publicUrl);
}