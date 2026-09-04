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

        // registrationNumber/city/country are all NOT NULL + UNIQUE (registrationNumber) columns
        // on `resellers` (see V10__create_resellers_tables.sql) and required (marked with a red
        // "*") on the become-reseller frontend form - but were never actually enforced here, so a
        // request missing one skipped straight past @Valid and hit the DB constraint instead,
        // surfacing as an unhelpful 500 rather than a clean 400.
        @NotBlank(message = "Le numéro d'enregistrement est obligatoire")
        String registrationNumber,

        @NotBlank(message = "La ville est obligatoire")
        String city,

        @NotBlank(message = "Le pays est obligatoire")
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