package com.guentours.storage.service;

import com.guentours.storage.StorageService;
import com.guentours.storage.domain.UserImage;
import com.guentours.storage.domain.UserImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ImageLibraryService {

    private final StorageService storageService;
    private final UserImageRepository userImageRepository;

    public ImageLibraryService(StorageService storageService, UserImageRepository userImageRepository) {
        this.storageService = storageService;
        this.userImageRepository = userImageRepository;
    }

    /**
     * Téléverse le fichier vers le stockage objet, puis enregistre les métadonnées liées
     * au propriétaire connecté. Chaque utilisateur ne voit et ne peut supprimer que ses
     * propres images (clé de partition owner_id).
     */
    @Transactional
    public UserImage upload(String ownerId, MultipartFile file) {
        String publicUrl = storageService.upload("users/" + ownerId, file);
        UserImage image = new UserImage(ownerId, publicUrl, file.getOriginalFilename(),
                file.getSize(), file.getContentType());
        return userImageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<UserImage> listForOwner(String ownerId) {
        return userImageRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional
    public void delete(String ownerId, String imageId) {
        UserImage image = userImageRepository.findByIdAndOwnerId(imageId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable"));
        storageService.delete(image.getUrl());
        userImageRepository.delete(image);
    }
}