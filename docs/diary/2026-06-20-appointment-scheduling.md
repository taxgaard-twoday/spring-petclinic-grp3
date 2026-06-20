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

## Step 3: Add appointment UI skeleton

**Author:** main

### Prompt Context

**Verbatim prompt:** PLEASE IMPLEMENT THIS PLAN:
# Plan: Angular Appointment UI Skeleton

## Summary
Add a top-nav AngularJS appointment screen that lets users choose an owner and pet, see vets, and view the selected pet’s existing appointments. This is a view-only skeleton: no booking submit, no cancellation action, and no available-slot API usage yet.

## Key Changes
- Add a new AngularJS `appointments` feature module under `spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments`.
- Register it in `app.js`, add script tags in `index.html`, and add a top-nav item in `scripts/fragments/nav.html`:
  - state: `appointments`
  - route: `/appointments`
  - template: `<appointments></appointments>`
- Appointment screen behavior:
  - Load owners from `api/customer/owners`.
  - Select an owner, then select one of that owner’s pets.
  - Load vets from `api/vet/vets` for display/selection context.
  - When a pet is selected, load appointments from `api/visit/owners/{ownerId}/pets/{petId}/appointments`.
  - Render upcoming appointments with vet id/name when resolvable, start, end, and status.
- Include booking-form scaffolding only as UI structure:
  - owner selector, pet selector, vet selector, date/time fields.
  - No create/cancel POST calls in this slice.
  - No disabled “Book” or “Cancel” workflow unless it is purely visual and cannot trigger backend mutation.
- Keep styling consistent with existing AngularJS/Bootstrap screens:
  - simple `h2` heading, Bootstrap form groups, table layout, and Font Awesome nav icon.
  - no new frontend framework or build tooling.

## Public Interfaces
- New browser route:
  - `#!/appointments`
- Existing backend APIs consumed:
  - `GET /api/customer/owners`
  - `GET /api/vet/vets`
  - `GET /api/visit/owners/{ownerId}/pets/{petId}/appointments`
- No backend API changes in this iteration.

## Test Plan
- Manual browser verification:
  - App loads without Angular module errors.
  - Top-nav “Appointments” route opens.
  - Owner selector populates from existing owners.
  - Pet selector updates when owner changes.
  - Vets load and display/select correctly.
  - Selecting a pet triggers the pet-scoped appointments GET.
  - Empty appointment responses render cleanly.
  - Existing owner, visits, and vets screens still work.
- Run API gateway tests if practical:
  - `.\mvnw.cmd -pl spring-petclinic-api-gateway test`
- Run visits-service tests only if the appointment API contract is touched:
  - `.\mvnw.cmd -pl spring-petclinic-visits-service test`

## Assumptions
- This slice intentionally does not implement booking, cancellation, or available-slot selection.
- The top-nav entry is the first UI entry point; pet-row links can be added later.
- `GET /api/customer/owners` includes pets, matching the existing owner list UI.
- Appointment status values remain `SCHEDULED` and `CANCELLED`.
- Update `docs/diary/2026-06-20-appointment-scheduling.md` with a new step after implementation.
**Interpretation:** Implement the approved AngularJS appointment UI skeleton in the API gateway static application.
**Inferred intent:** Add a reviewable frontend entry point for appointments that can exercise the new read API without adding booking or cancellation mutations yet.

### What I did

I added a new AngularJS feature module under `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments`: `appointments.js`, `appointments.component.js`, `appointments.controller.js`, and `appointments.template.html`. The route state is `appointments`, the browser route is `/appointments`, and the state renders `<appointments></appointments>`.

I registered the module in `/spring-petclinic-api-gateway/src/main/resources/static/scripts/app.js`, added the script tags to `/spring-petclinic-api-gateway/src/main/resources/static/index.html`, and added an Appointments top-nav item with a Font Awesome calendar icon in `/spring-petclinic-api-gateway/src/main/resources/static/scripts/fragments/nav.html`.

The controller loads owners from `api/customer/owners`, vets from `api/vet/vets`, updates the pet selector when an owner changes, and loads selected-pet appointments from `api/visit/owners/{ownerId}/pets/{petId}/appointments`. The template renders owner, pet, veterinarian, date, and time fields as scaffolding only, then shows an appointments table with resolved vet names when possible. It does not issue create or cancel POST calls.

I ran `.\mvnw.cmd -pl spring-petclinic-api-gateway test` and verified the Surefire reports for `/spring-petclinic-api-gateway/target/surefire-reports`. After replacing the literal form wrapper with a plain container, I reran `.\mvnw.cmd -q -pl spring-petclinic-api-gateway test`, which passed.

### Why

This gives the appointment API an Angular entry point without expanding the backend contract. The view-only screen is enough to validate routing, script registration, owner/pet context selection, vet display context, and the pet-scoped appointment list call before adding availability or mutation workflows.

### What worked

The existing static AngularJS pattern was easy to extend: feature modules live under `/spring-petclinic-api-gateway/src/main/resources/static/scripts`, `index.html` owns script registration, and `ui.router` states hang off the shared `app` parent. The final API gateway test reports showed 4 tests run with 0 failures, 0 errors, and 0 skipped.

### What didn't work

The command `.\mvnw.cmd -pl spring-petclinic-api-gateway test` completed with console output too large for the tool response, which reported `Output exceeded the available model context and was truncated`. I verified the result through the generated Surefire text reports instead: `ApiGatewayApplicationTests`, `VisitsServiceClientIntegrationTest`, and `ApiGatewayControllerTest` all reported 0 failures and 0 errors.

The follow-up command `.\mvnw.cmd -q -pl spring-petclinic-api-gateway test` first failed in the sandbox with `Exception in thread "main" java.net.SocketException: Permission denied: connect` while the Maven wrapper tried to access its distribution. I reran the same command with escalated approval and it passed.

### What I learned

The API gateway frontend does not have a bundling step for these screens, so every new AngularJS file needs explicit script registration in `index.html`. The owner API already provides pets, which keeps the first UI slice independent from any new customer-service call.

### What was tricky

The main design constraint was keeping the screen visibly ready for booking while ensuring it cannot mutate backend state. The date and time inputs are present as scaffolding, but there is no submit action, no Book button, no Cancel button, and no POST call in the controller.

### What warrants review

Review `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.controller.js` for the API calls and state reset behavior, `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.template.html` for the view-only booking scaffold, and the script/nav registration in `/spring-petclinic-api-gateway/src/main/resources/static/index.html`, `/spring-petclinic-api-gateway/src/main/resources/static/scripts/app.js`, and `/spring-petclinic-api-gateway/src/main/resources/static/scripts/fragments/nav.html`.

### Future work

The next UI slice can add available-slot loading once that API exists, then wire create and cancel actions with validation messages. A later polish pass can add pet-row appointment links from owner details if the top-nav flow feels too indirect.

## Step 4: Wire booking and cancellation UI

**Author:** main

### Prompt Context

**Verbatim prompt:** option 2
**Interpretation:** Implement Option 2 from the suggested next iterations: wire the Angular appointment skeleton to the existing create and cancel appointment APIs.
**Inferred intent:** Ship visible appointment booking and cancellation behavior in the UI before building available-slot selection or real species/working-hours validation.

### What I did

I updated `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.controller.js` to submit bookings to `POST api/visit/owners/{ownerId}/pets/{petId}/appointments`, format the API's `yyyy-MM-dd'T'HH:mm` start value from the date and time fields, cancel appointments through `POST api/visit/owners/{ownerId}/pets/{petId}/appointments/{appointmentId}/cancel`, refresh the appointment list after successful mutations, and show inline success or error messages.

I updated `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.template.html` from a view-only scaffold into a real booking form with required owner, pet, veterinarian, date, and time fields. The appointments table now shows a cancel button only for scheduled appointments that are at least 24 hours away according to the browser clock; the backend remains the authority for the cancellation rule.

I updated `/spring-petclinic-api-gateway/src/main/resources/static/scripts/infrastructure/httpErrorHandlingInterceptor.js` so requests can set `suppressGlobalErrorHandler` and handle errors locally. The global handler also now tolerates backend error payloads that have a `message` but no validation `errors` array.

I ran `.\mvnw.cmd -pl spring-petclinic-api-gateway test`, which passed with 4 tests, 0 failures, 0 errors, and 0 skipped.

### Why

The appointment UI already had the owner/pet/vet context and could list appointments. Wiring create and cancel gives users an end-to-end path through the lifecycle API while leaving available-slot discovery and real compatibility validation for the next backend iteration.

### What worked

The existing visits form pattern mapped cleanly to appointment creation: `ng-submit`, a simple `$http.post`, and a refresh after success. The API's short exception messages can be displayed directly inline once the appointment requests opt out of the global alert handler.

### What didn't work

No implementation errors blocked this step. The test run was verbose and still emitted existing warnings about `logback` ignoring `jmxConfigurator`, Mockito dynamic agent loading, and JVM class-data sharing, but the build ended with `BUILD SUCCESS`.

### What I learned

The existing global HTTP interceptor assumed validation-style error payloads with an `errors` array. Appointment rule failures from Spring's `@ResponseStatus` exceptions are simpler payloads, so locally handled appointment mutations need either an interceptor escape hatch or a more robust global handler.

### What was tricky

The cancellation button needed to avoid advertising an action the backend will reject, but the browser can only approximate the 24-hour window because the server clock is authoritative. The UI now hides cancel for appointments inside the browser-computed window and still surfaces backend errors if the server disagrees.

### What warrants review

Review `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.controller.js` for local date/time formatting and inline error handling, `/spring-petclinic-api-gateway/src/main/resources/static/scripts/appointments/appointments.template.html` for the form and cancel-button behavior, and `/spring-petclinic-api-gateway/src/main/resources/static/scripts/infrastructure/httpErrorHandlingInterceptor.js` for the opt-out behavior used by appointment requests.

### Future work

The next backend slice should add available-slot listing and enforce vet working hours plus pet species compatibility through real appointment context validation. After that, the UI can replace manual date/time entry with slot selection and reduce invalid booking attempts.

## Step 5: Add appointment availability API

**Author:** main

### Prompt Context

**Verbatim prompt:** PLEASE IMPLEMENT THIS PLAN:
# Plan: Appointment Availability API

## Summary
Add a pet-scoped availability endpoint in `visits-service` that returns valid 15-minute slots for one selected vet and date. This slice will add the domain calculation and tests now, using the existing appointment context port with fake test data; real HTTP integration to customers/vets remains the next slice.

## Key Changes
- Add `GET /owners/{ownerId}/pets/{petId}/appointments/available-slots?vetId=3&date=2026-07-01`.
- Response shape:
  ```json
  [
    { "start": "2026-07-01T09:00", "end": "2026-07-01T09:15" }
  ]
  ```
- Extend `AppointmentContextPort` from validate-only to return booking context:
  - vet working hours as `dayOfWeek`, `start`, `end`
  - production accepting adapter returns Monday-Friday `09:00-16:00`
  - service tests use fakes with narrower/custom vet hours
- Add `AppointmentService.availableSlots(ownerId, petId, vetId, date)`:
  - generate 15-minute candidates inside clinic hours and vet working hours
  - exclude slots before the 24h lead-time cutoff
  - exclude slots beyond the 3-month horizon
  - exclude weekends
  - exclude slots overlapping scheduled appointments for the selected vet
  - exclude slots overlapping scheduled appointments for the selected pet
  - return an empty list when no slots are available
- Reuse the same vet-working-hour check in `create(...)` so manual booking cannot bypass the availability rules.
- Keep species compatibility and real customer/vet HTTP validation out of this slice.

## Test Plan
- Add service tests with fixed `Clock`:
  - returns 15-minute slots for a valid weekday inside vet working hours
  - excludes slots outside vet working hours
  - excludes weekends, lead-time violations, and dates beyond 3 months
  - excludes slots already booked by the same vet
  - excludes slots already booked by the same pet with another vet
  - allows slots blocked only by cancelled appointments
  - rejects manual create when the selected start is outside vet working hours
- Add `@WebMvcTest` coverage for the available-slots endpoint and JSON shape.
- Run `.\mvnw.cmd -pl spring-petclinic-visits-service test`.

## Assumptions
- Availability is requested for a single date; the UI can call this per selected date later.
- Appointment times remain server-local `LocalDateTime` values using `yyyy-MM-dd'T'HH:mm`.
- Missing or invalid query params return Spring validation `400 Bad Request`.
- No API gateway route changes are needed because `/api/visit/**` already forwards to `visits-service`.
- Update `docs/diary/2026-06-20-appointment-scheduling.md` with a new step after implementation.
**Interpretation:** Implement the planned `visits-service` availability API using fake booking context data in tests and a permissive production context adapter.
**Inferred intent:** Move the feature from manual date/time entry toward backend-generated valid slots, while keeping real cross-service customer/vet integration for a separate reviewable slice.

### What I did

I added `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentSlot.java` as the domain value returned by availability. I extended `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentContextPort.java` with `bookingContext(...)`, `BookingContext`, and `VetWorkingHour` records, then updated `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AcceptingAppointmentContextPort.java` to return Monday-Friday `09:00-16:00` working hours.

I updated `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentService.java` with `availableSlots(ownerId, petId, vetId, date)`. It generates 15-minute clinic candidates for the requested date, filters them by scheduling window, vet working hours, and existing scheduled appointments for both vet and pet, and returns only available slots. I also reused the vet working-hour check in `create(...)` so manual booking cannot bypass the same rule.

I added `GET /owners/{ownerId}/pets/{petId}/appointments/available-slots` to `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/web/AppointmentResource.java`, with `vetId` and ISO `date` query parameters and `{ start, end }` response values formatted as `yyyy-MM-dd'T'HH:mm`.

I extended `/spring-petclinic-visits-service/src/test/java/org/springframework/samples/petclinic/visits/appointment/AppointmentServiceTest.java` with fixed-clock availability coverage and fake vet working hours, and extended `/spring-petclinic-visits-service/src/test/java/org/springframework/samples/petclinic/visits/web/AppointmentResourceTest.java` for the endpoint JSON shape. I ran `.\mvnw.cmd -pl spring-petclinic-visits-service test`.

### Why

The UI can already create and cancel appointments, but users still have to guess a valid date/time. This slice adds the backend capability needed to move the UI toward selecting valid slots, and it tightens manual booking by enforcing vet working hours even before real vets-service integration is wired.

### What worked

The existing repository overlap checks were reusable for availability because each generated slot is the same fixed 15-minute interval used by booking. The fake `AppointmentContextPort` in service tests made vet working hours easy to vary without introducing HTTP clients or service discovery into this slice. The final test run passed with `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

### What didn't work

No implementation errors blocked this step. The test run still emitted existing-style warnings about Mockito dynamic agent loading, `logback` ignoring `jmxConfigurator`, `Visit.date` using deprecated `@Temporal`, and Spring Cloud LoadBalancer's default cache, but none failed the build.

### What I learned

The appointment overlap methods already encode the key "scheduled only" distinction, so cancelled appointments naturally stop blocking availability as long as availability calls those methods with `AppointmentStatus.SCHEDULED`. The context port now needs to become the real customer/vet integration boundary in the next backend slice.

### What was tricky

The main nuance was sharing the scheduling-window logic between "is this candidate available?" and "is this requested booking valid?" without losing the specific error messages expected by create-time validation. Availability can quietly omit invalid candidates, while create still needs to throw short UI-readable errors.

### What warrants review

Review `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentService.java` for the slot-generation and vet-working-hour checks, `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/appointment/AppointmentContextPort.java` for the temporary booking-context shape, and `/spring-petclinic-visits-service/src/main/java/org/springframework/samples/petclinic/visits/web/AppointmentResource.java` for the public endpoint contract.

### Future work

The next slice should either wire real customer/vet HTTP validation behind `AppointmentContextPort` or update the Angular appointment UI to load available slots for the selected vet/date and book from those slots instead of free-form time entry.
