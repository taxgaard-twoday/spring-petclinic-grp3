# PRD: Appointment Scheduling

## Problem

PetClinic can record visits for pets, but it cannot schedule future appointments with veterinarians. Clinic users need owners to book only valid future appointment slots with veterinarians who handle the relevant animal species, while the system prevents double bookings and enforces cancellation rules.

## Relevant Codebase

- **What's there**: `spring-petclinic-visits-service` currently stores historical visits in `Visit`, with `id`, `visit_date`, `description`, and `pet_id` fields (`spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/model/Visit.java:32`, `spring-petclinic-visits-service/src/main/resources/db/hsqldb/schema.sql:3`). The visit model has no vet reference, time slot, appointment status, cancellation status, or availability concept.
- **What's there**: Visit APIs allow creating and reading visits for a pet, plus bulk lookup by pet ids (`spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/web/VisitResource.java:56`, `spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/web/VisitResource.java:72`).
- **What's there**: `spring-petclinic-vets-service` stores vets and treatment specialties. Vets have an eager many-to-many relationship to specialties through `vet_specialties` (`spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/model/Vet.java:52`, `spring-petclinic-vets-service/src/main/resources/db/hsqldb/schema.sql:18`). These existing specialties are not the same as the animal species a vet can handle for booking.
- **What's there**: The vets API currently exposes only `GET /vets`, returning all vets and their specialties (`spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/web/VetResource.java:34`, `spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/web/VetResource.java:44`).
- **What's there**: `spring-petclinic-customers-service` owns owners, pets, and pet types. A pet has a type and an owner (`spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/model/Pet.java:48`, `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/model/Pet.java:52`). A pet can be read by id through `owners/*/pets/{petId}` (`spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/web/PetResource.java:88`).
- **What's there**: The API gateway routes `/api/vet/**`, `/api/visit/**`, and `/api/customer/**` to the vets, visits, and customers services respectively (`spring-petclinic-api-gateway/src/main/resources/application.yml:21`, `spring-petclinic-api-gateway/src/main/resources/application.yml:27`, `spring-petclinic-api-gateway/src/main/resources/application.yml:33`).
- **How it works**: Existing visit creation is routed through the gateway as `/api/visit/owners/{ownerId}/pets/{petId}/visits`, stripped to `owners/{ownerId}/pets/{petId}/visits`, and persisted by `visits-service` after setting `petId` from the path.
- **How it works**: Existing owner detail composition in the gateway calls customers first, then calls visits using the owner's pet ids, and attaches returned visits to the owner response (`spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/boundary/web/ApiGatewayController.java:54`, `spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/application/VisitsServiceClient.java:42`).
- **Patterns to follow**: Service APIs are small Spring REST controllers using repository-backed domain models. Existing controller tests use `@WebMvcTest` with mocked repositories, as in `VisitResourceTest` and `VetResourceTest`.
- **Integration points**: Appointment scheduling must integrate with vet handled-species data, pet species data, gateway routing, service schemas, seed data, and docker-compose demonstrability.

## Goal

Owners can use API calls to find available appointment slots for a selected vet, book a valid future appointment for a pet, and cancel an appointment when allowed. The system enforces clinic hours, vet working hours, pet species compatibility, lead time, booking horizon, and double-booking prevention.

## User Stories

- As a pet owner, I want to see available appointment slots for a selected veterinarian so that I can choose a valid time before booking.
- As a pet owner, I want to book a future appointment for my pet with a vet who handles that animal species so that the appointment is clinically appropriate.
- As a pet owner, I want invalid booking attempts to return clear API errors so that a client can explain what must be changed.
- As a pet owner, I want to cancel an appointment at least 24 hours before it starts so that the clinic can release the slot.
- As PetClinic, I want the system to prevent double bookings so that a veterinarian cannot have overlapping appointments.
- As PetClinic, I want sample appointment, vet, working-hour, and species data so that the scheduling behavior can be demonstrated with API calls and docker compose.

## Acceptance Criteria

1. Appointments are represented as a new scheduling concept separate from historical visits.
2. The appointment API allows an API user to list available 15-minute slots for a selected vet.
3. The appointment API allows an API user to book an appointment for a pet, vet, and available slot.
4. The appointment API allows an API user to cancel an appointment only when the appointment starts at least 24 hours after the cancellation request.
5. Clinic opening hours are Monday through Friday, 09:00 to 16:00.
6. Appointment slots are 15 minutes long and must fit within clinic opening hours.
7. Appointment slots must also fit within the selected vet's working hours.
8. Booking is rejected for any slot in the past.
9. Booking is rejected for any slot less than 24 hours in the future.
10. Booking is rejected for any slot more than 3 months in the future.
11. Booking is rejected when the selected vet already has a non-cancelled appointment in the same slot.
12. Booking is rejected when the selected vet does not handle the pet's animal species.
13. Vets have separate handled-species data for booking eligibility, limited to: `cat`, `dog`, `gold fish`, `birds`, `giraf`, `pig`, and `small humans`.
14. Existing treatment-oriented specialties such as `radiology`, `surgery`, and `dentistry` remain in place and are not used for booking compatibility.
15. Error responses for invalid booking and cancellation attempts are understandable enough for an API client to react to the reason.
16. Sample data exists for vet handled species, vet working hours, and appointment scenarios.
17. The completed feature can be demonstrated via API calls through the gateway and with services running under docker compose.
18. No Angular or browser UI changes are required.

## Scope

### In scope

- API-only appointment scheduling.
- New appointment persistence and API surface.
- Vet working-hour data needed to validate and list slots.
- Clinic opening-hour validation.
- Vet handled-species data and compatibility checks against pet species.
- Double-booking prevention for active appointments.
- Cancellation rules and cancelled appointment state.
- Sample data for demonstration.
- Tests covering booking, availability, cancellation, invalid species compatibility, invalid times, and double-booking prevention.
- Gateway-accessible endpoints for demonstration.

### Out of scope

- Angular frontend screens for appointment scheduling.
- Replacing existing visit history behavior.
- Appointment reminders, notifications, payments, staff calendars, recurring schedules, waitlists, and authentication or authorization.
- Treatment planning beyond handled-species vet eligibility.

## Risks

- The existing `visits-service` name and visit model may invite mixing historical visits with future appointments; the feature must keep appointment scheduling conceptually separate.
- Vet specialty data currently contains treatment-oriented specialties, so booking compatibility must use separate handled-species data instead of overloading the existing specialty model.
- Pet types in existing seed data include species outside the requested handled-species list, so sample data must make demonstration cases clear.
- Availability requires combining clinic hours, vet working hours, appointment duration, lead time, booking horizon, and existing appointment state; these rules should be tested together, not only independently.
- Double-booking prevention must hold under persistence constraints or transactional checks, not only through a pre-save query.
- Docker compose currently references published `springcommunity/*` images; demonstration of local changes may require an updated build-and-run path or locally built images.
