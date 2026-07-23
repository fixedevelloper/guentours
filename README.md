# Module Partners — Guen Tours

Module Spring Boot / Spring Modulith gérant l'enregistrement des partenaires
(compagnies aériennes, hôtels, agences de location de voiture, agences de
location meublée) et leur inventaire respectif.

## Structure

```
com.guentours.partners
├── domain / repository / service / web / event   → enregistrement & validation des partenaires
├── flight            → vols, tarifs, disponibilités (compagnies)
├── hotel             → hôtels, types de chambre, disponibilités
├── carrental         → véhicules, disponibilités
└── furnishedrental   → logements meublés, disponibilités

com.guentours.user        → rôles partenaires, création de compte à l'approbation
com.guentours.notification → email de bienvenue partenaire (Thymeleaf)
```

## Flux d'approbation

1. `POST /api/partners/register` — un partenaire s'inscrit (statut `PENDING_REVIEW`).
2. Un admin appelle `PATCH /api/partners/{id}/approve`.
3. `PartnerApprovedEvent` est publié (Spring Modulith `@ApplicationModuleListener`,
   transactionnel + rejouable en cas d'échec).
4. `com.guentours.user` crée le compte partenaire avec le bon rôle
   (`PARTNER_AIRLINE`, `PARTNER_HOTEL`, `PARTNER_CAR_RENTAL`, `PARTNER_FURNISHED_RENTAL`).
5. `com.guentours.notification` envoie l'email de bienvenue avec mot de passe temporaire.

## Endpoints CRUD par verticale

- `/api/partners/{partnerId}/flights` — vols, tarifs (`/fares`), disponibilités (`/fares/{id}/availability`)
- `/api/partners/{partnerId}/hotels` — hôtels, types de chambre (`/room-types`), disponibilités
- `/api/partners/{partnerId}/vehicles` — véhicules, disponibilités
- `/api/partners/{partnerId}/properties` — logements meublés, disponibilités

## À configurer avant démarrage

- Variables d'env : `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- Ajouter la config de sécurité JWT existante du projet (non incluse ici) avec
  `permitAll` sur `POST /api/partners/register` et `hasRole(...)` sur le reste.
- Adapter `spring.jpa.hibernate.ddl-auto` selon l'environnement (les migrations
  Flyway dans `db/migration` créent déjà tout le schéma).
