package com.guentours.storage.web;

import com.guentours.security.AppUserPrincipal;
import com.guentours.storage.domain.UserImage;
import com.guentours.storage.service.ImageLibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class UserImageController {

    private final ImageLibraryService service;

    public UserImageController(ImageLibraryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserImageResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String ownerId = ownerId(authentication);
        return UserImageResponse.from(service.upload(ownerId, file));
    }

    @GetMapping
    public List<UserImageResponse> list(Authentication authentication) {
        String ownerId = ownerId(authentication);
        return service.listForOwner(ownerId).stream().map(UserImageResponse::from).toList();
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String imageId, Authentication authentication) {
        service.delete(ownerId(authentication), imageId);
    }

    /** ⚠️ Suppose que AppUserPrincipal expose getUserId() — à confirmer/ajuster. */
    private String ownerId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUserId();
    }
}