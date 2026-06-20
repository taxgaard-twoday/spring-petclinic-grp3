package org.springframework.samples.petclinic.visits.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.visits.appointment.AppointmentSlot;
import org.springframework.samples.petclinic.visits.appointment.AppointmentService;
import org.springframework.samples.petclinic.visits.model.Appointment;
import org.springframework.samples.petclinic.visits.model.AppointmentStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentResource.class)
@ActiveProfiles("test")
class AppointmentResourceTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AppointmentService appointmentService;

    @Test
    void shouldListAppointmentsForPet() throws Exception {
        given(appointmentService.findForPet(7)).willReturn(List.of(
            appointment(1, 7, 3, LocalDateTime.of(2026, 7, 1, 9, 0), AppointmentStatus.SCHEDULED)
        ));

        mvc.perform(get("/owners/6/pets/7/appointments").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].petId").value(7))
            .andExpect(jsonPath("$[0].vetId").value(3))
            .andExpect(jsonPath("$[0].start").value("2026-07-01T09:00"))
            .andExpect(jsonPath("$[0].end").value("2026-07-01T09:15"))
            .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void shouldListAvailableSlotsForPetAndVet() throws Exception {
        given(appointmentService.availableSlots(6, 7, 3, LocalDate.of(2026, 7, 1))).willReturn(List.of(
            new AppointmentSlot(LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 7, 1, 9, 15))
        ));

        mvc.perform(get("/owners/6/pets/7/appointments/available-slots")
                .param("vetId", "3")
                .param("date", "2026-07-01")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].start").value("2026-07-01T09:00"))
            .andExpect(jsonPath("$[0].end").value("2026-07-01T09:15"));
    }

    @Test
    void shouldCreateAppointment() throws Exception {
        given(appointmentService.create(eq(6), eq(7), eq(3), any(LocalDateTime.class)))
            .willReturn(appointment(1, 7, 3, LocalDateTime.of(2026, 7, 1, 9, 0), AppointmentStatus.SCHEDULED));

        mvc.perform(post("/owners/6/pets/7/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "vetId": 3,
                      "start": "2026-07-01T09:00"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.petId").value(7))
            .andExpect(jsonPath("$.vetId").value(3))
            .andExpect(jsonPath("$.start").value("2026-07-01T09:00"))
            .andExpect(jsonPath("$.end").value("2026-07-01T09:15"))
            .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void shouldCancelAppointment() throws Exception {
        given(appointmentService.cancel(7, 1))
            .willReturn(appointment(1, 7, 3, LocalDateTime.of(2026, 7, 1, 9, 0), AppointmentStatus.CANCELLED));

        mvc.perform(post("/owners/6/pets/7/appointments/1/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturnBadRequestForInvalidAppointment() throws Exception {
        given(appointmentService.create(eq(6), eq(7), eq(3), any(LocalDateTime.class)))
            .willThrow(new BadRequestException("Appointment must start on a 15-minute boundary"));

        mvc.perform(post("/owners/6/pets/7/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "vetId": 3,
                      "start": "2026-07-01T09:10"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictForDoubleBooking() throws Exception {
        given(appointmentService.create(eq(6), eq(7), eq(3), any(LocalDateTime.class)))
            .willThrow(new ConflictException("Pet already has an appointment in this slot"));

        mvc.perform(post("/owners/6/pets/7/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "vetId": 3,
                      "start": "2026-07-01T09:00"
                    }
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnNotFoundForMissingAppointmentCancellation() throws Exception {
        given(appointmentService.cancel(7, 99)).willThrow(new ResourceNotFoundException("Appointment 99 not found"));

        mvc.perform(post("/owners/6/pets/7/appointments/99/cancel"))
            .andExpect(status().isNotFound());
    }

    private Appointment appointment(int id, int petId, int vetId, LocalDateTime start, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPetId(petId);
        appointment.setVetId(vetId);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(15));
        appointment.setStatus(status);
        return appointment;
    }
}
