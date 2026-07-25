package com.guentours.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private final MinioClient client;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String upload(String keyPrefix, MultipartFile file) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = "%s/%s%s".formatted(keyPrefix, UUID.randomUUID(), extension);

        try (InputStream stream = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(stream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to upload object '{}' to MinIO: {}", objectKey, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Échec de l'upload du fichier");
        }

        return "%s/%s/%s".formatted(properties.getPublicUrl(), properties.getBucket(), objectKey);
    }

    @Override
    public void delete(String publicUrl) {
        String objectKey = extractObjectKey(publicUrl);
        if (objectKey == null) {
            return;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to delete object '{}' from MinIO: {}", objectKey, ex.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide ou manquant");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (max 5 Mo)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format non supporté (JPEG, PNG ou WebP uniquement)");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }

    private String extractObjectKey(String publicUrl) {
        String prefix = "%s/%s/".formatted(properties.getPublicUrl(), properties.getBucket());
        if (publicUrl == null || !publicUrl.startsWith(prefix)) {
            return null;
        }
        return publicUrl.substring(prefix.length());
    }
}