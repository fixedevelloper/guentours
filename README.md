# Guentours API

Backend for a flight + hotel booking platform: it fans a single search out to
multiple travel APIs (Travelopro, Sabre, Travelport), harmonizes their
responses into one price-ranked list, and takes a selected offer through a
secure checkout - payment - provider confirmation - e-ticketing flow, with
live status tracking and automatic account provisioning for guest checkouts.

Built with **Spring Modulith**, **Spring Data JPA / MySQL** and **Spring
Security (JWT)**.

## Architecture

The application is a single Spring Boot deployable, organized as Spring
Modulith modules (one top-level package per module under `com.guentours`):

| Module | Responsibility |
|---|---|
| `shared` | Cross-cutting value objects (`Money`), exceptions, global error handling. Open module - everyone may depend on it. |
| `user` | Account lifecycle, including transparent auto-provisioning during guest checkout. |
| `security` | Password hashing, JWT issuance/validation, the Spring Security filter chain. |
| `provider` | The `TravelProviderClient` SPI plus one adapter each for Travelopro, Sabre and Travelport. |
| `search` | Fans a search out to every enabled provider in parallel and harmonizes the raw offers. |
| `booking` | Owns the booking aggregate/state machine and real-time status tracking (SSE). |
| `payment` | Charges the authoritative booking price via a pluggable `PaymentGateway`. |
| `ticketing` | Generates electronic tickets once a booking is provider-confirmed. |
| `notification` | Sends transactional emails, driven entirely by domain events. |

Module boundaries are enforced by `ModularityTests` (`ApplicationModules.verify()`),
which fails the build if a module reaches into another module's internals.

### Search -> harmonization

`FlightSearchService`/`HotelSearchService` dispatch the search to every
enabled `TravelProviderClient` concurrently (`CompletableFuture` on a bounded
pool), then `FlightHarmonizer`/`HotelHarmonizer` group the raw offers by
physical product (same airline+flight number+route+date, or same
hotel+room+stay dates) and keep every provider's price, sorted cheapest
first. Every individual offer is cached server-side for ~20 minutes behind an
opaque `offerId` (`OfferCache`) - checkout only ever trusts the price the
*server* quoted, never one a client could tamper with.

### Checkout -> payment -> provider confirmation -> e-ticket

1. `POST /api/bookings/checkout` resolves the offer from the cache, looks up
   or **auto-creates** the account for the contact email
   (`UserService#findOrCreateForCheckout`), and persists a `Booking` in
   `PENDING_PAYMENT`.
2. `POST /api/payments` re-fetches the booking's authoritative price,
   charges it through `PaymentGateway`, marks the booking `PAID`, and
   publishes `BookingPaidEvent`.
3. `BookingConfirmationListener` picks that event up after commit and, on a
   background thread, calls the same provider that quoted the offer to
   confirm the reservation, moving the booking through `CONFIRMING` to
   `CONFIRMED` (or `FAILED`).
4. `ticketing` and `notification` react to `BookingConfirmedEvent`/
   `BookingFailedEvent` to generate e-tickets and send emails.
5. `GET /api/bookings/{id}/track` streams every status transition over
   Server-Sent Events so the client sees progress live instead of polling.

### Provider adapters

Each of `TraveloproClient`, `SabreClient` and `TravelportClient` implements
the same `TravelProviderClient` SPI. Until real sandbox/production
credentials are configured (`app.providers.<name>.mock-mode=true`, the
default), each adapter returns deterministic simulated offers instead of
calling the vendor - airlines/hotels that legitimately overlap across
providers are seeded identically so harmonization has real duplicates to
merge, only the price differs per provider. Flipping `mock-mode` to `false`
routes calls through the adapter's `callFlightApi`/`callHotelApi` methods,
which are the integration points left for mapping each vendor's actual
request/response contract.

### Security notes

- Card numbers/CVVs are handed to `PaymentGateway` and never persisted -
  only the last 4 digits and the gateway's own reference are stored.
- A temporary password generated for an auto-provisioned account is **never**
  put on the Spring application-event bus, because Spring Modulith durably
  persists event payloads in the `event_publication` table - the wrong place
  for a plaintext credential. It's held in a short-lived, single-read,
  in-memory cache (`TemporaryPasswordCache`) instead; `UserAutoProvisionedEvent`
  only carries the user id.
- Domain events generally carry just an aggregate id, not a data snapshot -
  listeners fetch what they need through the owning module's service. This
  keeps the durably-persisted event payloads small and avoids stale data.

## Running locally

Prerequisites: JDK 21, Maven, a MySQL instance (or override `DB_URL` to point
elsewhere), and an SMTP relay if you want emails to actually deliver (a local
[MailHog](https://github.com/mailhog/MailHog)/Mailpit on port 1025 works well
for development).

```bash
mvn spring-boot:run
```

Key environment variables (see `application.yml` for the full list and defaults):

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection |
| `APP_JWT_SECRET` | Base64 HMAC secret for JWTs - **override in production** |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP relay |
| `TRAVELOPRO_*`, `SABRE_*`, `TRAVELPORT_*` | Per-provider `enabled`/`mock-mode`/`base-url`/`api-key`/`api-secret` |

## API overview

| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | public | Self-service account + JWT |
| `GET /api/search/flights`, `GET /api/search/hotels` | public | Harmonized multi-provider search |
| `POST /api/bookings/checkout` | public (guest checkout) | Create a pending booking, auto-provisioning the account |
| `GET /api/bookings/{id}` | public | Booking status/details |
| `GET /api/bookings/{id}/track` | public | Server-Sent Events status stream |
| `POST /api/payments` | public | Charge a booking and trigger provider confirmation |
| `GET /api/tickets/booking/{id}` | public | Issued e-tickets |

Guest checkout is a first-class flow, so booking/payment/ticket lookups stay
public in `SecurityConfig`. Before going to production, scope those lookups
to the owning email or a signed tracking token instead of a bare id.

## Tests

```bash
mvn test
```

- `ModularityTests` verifies the module structure (no illegal cross-module
  reach-ins, no cycles) and generates a PlantUML snapshot under `target/spring-modulith-docs`.
- `FlightHarmonizerTest` unit-tests the price-merging logic.
- `BookingFlowIntegrationTest` runs the full flow - search, checkout, payment,
  async provider confirmation, e-ticket generation - against an in-memory H2
  database with the provider adapters in mock mode.
