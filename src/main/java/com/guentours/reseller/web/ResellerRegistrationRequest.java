package com.guentours.reseller.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResellerRegistrationRequest(
        String userId,

        @NotBlank(message = "Le nom de la société est obligatoire")
        String companyName,

        @NotBlank(message = "Le nom du contact est obligatoire")
        String contactName,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Le format de l'email est invalide")
        String email,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String phone,

        String registrationNumber,
        String city,
        String country,
        String logoUrl
) {
    /**
     * Crée une copie de la requête en injectant l'URL du logo après son téléversement sur MinIO.
     */
    public ResellerRegistrationRequest withLogoUrl(String uploadedLogoUrl) {
        return new ResellerRegistrationRequest(
                this.userId,
                this.companyName,
                this.contactName,
                this.email,
                this.phone,
                this.registrationNumber,
                this.city,
                this.country,
                uploadedLogoUrl
        );
    }
}