package org.springframework.samples.petclinic.visits.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.visits.appointment.AppointmentSlot;
import org.springframework.samples.petclinic.visits.appointment.AppointmentService;
import org.springframework.samples.petclinic.visits.model.Appointment;
import org.springframework.samples.petclinic.visits.model.AppointmentStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@Timed("petclinic.appointment")
class AppointmentResource {

    private static final Logger log = LoggerFactory.getLogger(AppointmentResource.class);

    private final AppointmentService appointmentService;

    AppointmentResource(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("owners/{ownerId}/pets/{petId}/appointments")
    public List<AppointmentResponse> read(
        @PathVariable("ownerId") @Min(1) int ownerId,
        @PathVariable("petId") @Min(1) int petId) {

        return appointmentService.findForPet(petId).stream()
            .map(AppointmentResponse::from)
            .toList();
    }

    @GetMapping("owners/{ownerId}/pets/{petId}/appointments/available-slots")
    public List<AvailableSlotResponse> availableSlots(
        @PathVariable("ownerId") @Min(1) int ownerId,
        @PathVariable("petId") @Min(1) int petId,
        @RequestParam("vetId") @Min(1) int vetId,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate date) {

        return appointmentService.availableSlots(ownerId, petId, vetId, date).stream()
            .map(AvailableSlotResponse::from)
            .toList();
    }

    @PostMapping("owners/{ownerId}/pets/{petId}/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(
        @Valid @RequestBody AppointmentRequest request,
        @PathVariable("ownerId") @Min(1) int ownerId,
        @PathVariable("petId") @Min(1) int petId) {

        log.info("Booking appointment for pet {} with vet {} at {}", petId, request.vetId(), request.start());
        Appointment appointment = appointmentService.create(ownerId, petId, request.vetId(), request.start());
        return AppointmentResponse.from(appointment);
    }

    @PostMapping("owners/{ownerId}/pets/{petId}/appointments/{appointmentId}/cancel")
    public AppointmentResponse cancel(
        @PathVariable("ownerId") @Min(1) int ownerId,
        @PathVariable("petId") @Min(1) int petId,
        @PathVariable("appointmentId") @Min(1) int appointmentId) {

        log.info("Cancelling appointment {} for pet {}", appointmentId, petId);
        Appointment appointment = appointmentService.cancel(petId, appointmentId);
        return AppointmentResponse.from(appointment);
    }

    record AppointmentRequest(
        @Min(1) int vetId,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime start
    ) {
    }

    record AppointmentResponse(
        Integer id,
        int petId,
        int vetId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime end,
        AppointmentStatus status
    ) {

        static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(
                appointment.getId(),
                appointment.getPetId(),
                appointment.getVetId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
            );
        }
    }

    record AvailableSlotResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime start,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime end
    ) {

        static AvailableSlotResponse from(AppointmentSlot slot) {
            return new AvailableSlotResponse(slot.start(), slot.end());
        }
    }
}
