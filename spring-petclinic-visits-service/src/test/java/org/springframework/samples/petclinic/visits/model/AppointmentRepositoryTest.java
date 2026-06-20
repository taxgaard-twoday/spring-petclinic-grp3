package org.springframework.samples.petclinic.visits.model;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    AppointmentRepository appointmentRepository;

    @Test
    void shouldPersistAppointmentAndFindOverlaps() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 9, 0);
        Appointment appointment = new Appointment();
        appointment.setPetId(7);
        appointment.setVetId(3);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(15));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appointment);

        assertThat(saved.getId()).isNotNull();
        assertThat(appointmentRepository.findByPetIdOrderByStartTimeAsc(7)).hasSize(1);
        assertThat(appointmentRepository.existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            3, AppointmentStatus.SCHEDULED, start.plusMinutes(15), start)).isTrue();
        assertThat(appointmentRepository.existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            7, AppointmentStatus.SCHEDULED, start.plusMinutes(15), start)).isTrue();
    }
}
