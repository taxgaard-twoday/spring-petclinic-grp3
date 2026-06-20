# Diary: Appointment scheduling

Implementation diary for the appointment scheduling task. This entry covers the first implementation slice: adding booking-facing vet handled species and working hours to `vets-service` without changing existing treatment specialties.

## Step 1: Add vets booking profile foundation

**Author:** main

### Prompt Context

**Verbatim prompt:** continue
**Interpretation:** Continue from the approved Option 1 plan now that Default mode is active.
**Inferred intent:** Implement the vets-service foundation for appointment scheduling: handled species, vet working hours, a booking profile API, sample data, and tests.

### What I did

I added `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/model/HandledSpecies.java` and `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/model/VetWorkingHour.java`, then connected them to `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/model/Vet.java`. The existing treatment specialties remain unchanged; handled species and working hours are ignored on the existing `/vets` JSON response.

I updated both database variants in `/spring-petclinic-vets-service/src/main/resources/db/hsqldb/schema.sql`, `/spring-petclinic-vets-service/src/main/resources/db/hsqldb/data.sql`, `/spring-petclinic-vets-service/src/main/resources/db/mysql/schema.sql`, and `/spring-petclinic-vets-service/src/main/resources/db/mysql/data.sql`. The seed data adds the requested species list, vet-to-species assignments, and varied weekday working hours within 09:00-16:00.

I added `GET /vets/{vetId}/booking-profile` in `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/web/VetResource.java` and a local `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/web/ResourceNotFoundException.java` for 404s.

I extended `/spring-petclinic-vets-service/src/test/java/org/springframework/samples/petclinic/vets/web/VetResourceTest.java` and added `/spring-petclinic-vets-service/src/test/java/org/springframework/samples/petclinic/vets/model/VetRepositoryTest.java`. I ran `.\mvnw.cmd -pl spring-petclinic-vets-service test`, which passed with 4 tests.

### Why

This creates the upstream contract needed by appointment scheduling: a caller can ask which animal species a vet handles and when that vet works, without overloading the existing treatment specialty model or changing the existing Angular vet list behavior.

### What worked

The existing JPA entity style was straightforward to extend. `@JsonIgnore` on the new `Vet` getters kept `GET /vets` stable while still allowing the new booking profile DTO to use the data internally. The HSQL seed data loaded successfully in the repository test.

### What didn't work

The command `rg -n "ResourceNotFoundException|ResponseStatus\(value = HttpStatus.NOT_FOUND\)|@DataJpaTest|@SpringBootTest|sql.init|ddl-auto" spring-petclinic-*\src\test spring-petclinic-*\src\main\java` failed on Windows with `rg: spring-petclinic-*\src\test: The filename, directory name, or volume label syntax is incorrect. (os error 123)`. I reran the search with explicit directories.

The first test run, `.\mvnw.cmd -pl spring-petclinic-vets-service test`, failed because the Maven wrapper needed network access: `Exception in thread "main" java.net.SocketException: Permission denied: connect`. I reran the same command with escalated approval and it passed.

The first successful test run still fetched config from `http://localhost:8888/`, so I tightened `VetRepositoryTest` with explicit Spring test properties to disable config import, config client, discovery, and Eureka. The next `.\mvnw.cmd -pl spring-petclinic-vets-service test` run passed without fetching config server.

The command `rg -n "interface DataJpaTest|class DataJpaTest|@interface DataJpaTest" "$env:USERPROFILE\.m2\repository\org\springframework\boot"` timed out while checking the local Maven repository. I switched to inspecting the Boot test autoconfigure jar directly.

### What I learned

Spring Boot 4 has split some test slices into dedicated artifacts; this module did not have the JPA test slice annotation readily available through its current test classpath. A focused `@SpringBootTest` with config/discovery disabled was the least invasive way to verify seeded repository data without changing dependencies.

### What was tricky

The main sharp edge was preserving the existing `/vets` response while adding relationships to `Vet`. Returning JPA entities directly means any public getter can become API surface, so the new data needed explicit JSON ignoring and a separate DTO for the booking endpoint.

### What warrants review

Review the new booking profile JSON shape in `/spring-petclinic-vets-service/src/main/java/org/springframework/samples/petclinic/vets/web/VetResource.java`, the seed assignments in both HSQL and MySQL data files, and whether the chosen varied working hours are good enough for the next availability iteration.

### Future work

The next iteration can consume `GET /vets/{vetId}/booking-profile` from appointment scheduling, add appointment persistence, and use the handled species plus working hours to validate booking and availability rules.

## Step 2: Add appointment core API

**Author:** main

### Prompt Context

**Verbatim prompt:** PLEASE IMPLEMENT THIS PLAN:
# Plan: Appointment Core API In Visits Service

## Summary
Add appointment lifecycle support to `visits-service` as a new concept beside historical visits. This slice will expose pet-scoped APIs to create, list, and cancel appointments, enforce local scheduling rules, and leave real customers/vets HTTP validation for the next iteration behind explicit ports.

## Key Changes
- Add a new appointment domain in `visits-service`:
  - `Appointment` entity with `id`, `petId`, `vetId`, `startTime`, `endTime`, and `status`.
  - Status values: `SCHEDULED` and `CANCELLED`.
  - Duration is always 15 minutes; clients send only `start`.
- Update HSQLDB and MySQL schema/data:
  - New `appointments` table beside `visits`.
  - Keep appointment seed data empty unless deterministic test/demo data can be created without time-sensitive dates.
- Add pet-scoped REST endpoints:
  - `GET /owners/{ownerId}/pets/{petId}/appointments`
  - `POST /owners/{ownerId}/pets/{petId}/appointments`
    ```json
    { "vetId": 1, "start": "2026-07-01T09:00" }
    ```
  - `POST /owners/{ownerId}/pets/{petId}/appointments/{appointmentId}/cancel`
  - Gateway paths will be under `/api/visit/...` through the existing route.
- Response shape:
  ```json
  {
    "id": 1,
    "petId": 7,
    "vetId": 1,
    "start": "2026-07-01T09:00",
    "end": "2026-07-01T09:15",
    "status": "SCHEDULED"
  }
  ```
- Add `AppointmentService` rules:
  - Monday-Friday only.
  - Start must be between `09:00` and `15:45` so the 15-minute appointment fits before `16:00`.
  - Start must align to 15-minute boundaries.
  - Start must be at least 24 hours after “now”.
  - Start must be no more than 3 months after “now”.
  - Reject overlapping non-cancelled appointments for the same vet.
  - Reject overlapping non-cancelled appointments for the same pet, even with a different vet.
  - Cancellation allowed only when appointment start is at least 24 hours after “now”.
- Add explicit ports for later cross-service validation:
  - `PetLookupPort` and `VetBookingProfilePort`, or one combined `AppointmentContextPort`.
  - In this slice, use a simple production implementation that accepts context checks, and service tests use fakes to prove the port is called.
  - Real HTTP calls to customers-service and vets-service are deferred.

## Error Behavior
- Invalid time, slot alignment, horizon, or cancellation window: `400 Bad Request`.
- Double booking for the same vet or the same pet: `409 Conflict`.
- Missing appointment on cancel: `404 Not Found`.
- Error messages should be short and UI-readable, for example `Appointment must start on a 15-minute boundary`.

## Test Plan
- Add service tests with a fixed `Clock`:
  - creates a valid appointment and computes `end`.
  - rejects past, less-than-24-hour, more-than-3-month, weekend, outside-hours, and non-15-minute starts.
  - rejects overlapping scheduled appointments for the same vet.
  - rejects overlapping scheduled appointments for the same pet, even when the vet is different.
  - allows same slot for a different vet only when the pet is also different.
  - allows booking when an existing appointment in the slot is cancelled.
  - cancels an eligible appointment.
  - rejects cancellation inside the 24-hour window.
- Add `@WebMvcTest` coverage for list/create/cancel endpoint response shapes and status codes.
- Add repository/schema coverage if needed to verify HSQLDB table mapping.
- Run `.\mvnw.cmd -pl spring-petclinic-visits-service test`.

## Assumptions
- This slice is lifecycle-only; available-slot listing is a separate next iteration.
- Pet/vet species compatibility is not enforced yet; the ports are added now so real integration can land cleanly next.
- Appointment times use server-local `LocalDateTime` with `yyyy-MM-dd'T'HH:mm` JSON values.
- Existing visit APIs and visit persistence remain unchanged.
- The implementation should update `docs/diary/2026-06-20-appointment-scheduling.md` with a new step after the code changes.
**Interpretation:** Implement the approved appointment core API plan in `visits-service`.
**Inferred intent:** Add appointment persistence and lifecycle endpoints while preserving existing visit behavior and deferring real cross-service compatibility validation.

### What I did

I added the appointment domain to `/spring-petclinic-visits-service`: `Appointment`, `AppointmentStatus`, and `AppointmentRepository` under `/src/main/java/org/springframework/samples/petclinic/visits/model`. I added `/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentService.java` with the local scheduling rules, plus `AppointmentContextPort` and an accepting production adapter as the future customer/vet validation seam. I added `/src/main/java/org/springframework/samples/petclinic/visits/config/ClockConfig.java` so time rules can use an injectable `Clock`.

I added `/src/main/java/org/springframework/samples/petclinic/visits/web/AppointmentResource.java` with pet-scoped list, create, and cancel endpoints, plus `BadRequestException`, `ConflictException`, and `ResourceNotFoundException` for the planned status codes. I updated `/spring-petclinic-visits-service/src/main/resources/db/hsqldb/schema.sql` and `/spring-petclinic-visits-service/src/main/resources/db/mysql/schema.sql` with an `appointments` table. I intentionally left appointment seed data empty.

I added tests in `/spring-petclinic-visits-service/src/test/java/org/springframework/samples/petclinic/visits/appointment/AppointmentServiceTest.java`, `/spring-petclinic-visits-service/src/test/java/org/springframework/samples/petclinic/visits/web/AppointmentResourceTest.java`, and `/spring-petclinic-visits-service/src/test/java/org/springframework/samples/petclinic/visits/model/AppointmentRepositoryTest.java`. I ran `.\mvnw.cmd -pl spring-petclinic-visits-service test`.

### Why

This creates the backend lifecycle the UI needs before available-slot and Angular booking work. It keeps appointments separate from visits, gives future UI code stable pet-scoped API endpoints, and adds the explicit port seam needed for later real pet/vet compatibility checks.

### What worked

The service layer stayed compact because the current slice is lifecycle-only. The fixed-clock service tests made the time rules deterministic. The repository test verified the HSQLDB schema and Spring Data overlap queries. The final test run passed with `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

### What didn't work

No implementation errors blocked this step. The test run produced existing-style warnings about Mockito dynamic agent loading, `logback` ignoring `jmxConfigurator`, and deprecated `@Temporal` usage on the existing `Visit.date` field, but none were introduced as failing conditions by this change.

### What I learned

The existing `visits-service` was deliberately thin, so putting appointment behavior into a separate service/resource path avoided making `VisitResource` carry scheduling concerns. The repository-level overlap checks are expressive enough for the current fixed 15-minute appointment model.

### What was tricky

The most important rule nuance was double booking by pet as well as by vet: a different vet does not make the same slot valid if the pet is already scheduled. The service now checks both dimensions before saving.

### What warrants review

Review the API shape in `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/web/AppointmentResource.java`, the rule ordering and messages in `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentService.java`, and the overlap query method names in `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/model/AppointmentRepository.java`.

### Future work

The next slice should add either available-slot listing or real cross-service validation against `customers-service` pet data and `vets-service` booking profiles. The Angular booking UI should wait until at least availability exists, unless the team wants a UI skeleton first.
