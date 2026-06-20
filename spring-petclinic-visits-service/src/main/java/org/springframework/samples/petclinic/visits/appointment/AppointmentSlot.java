package org.springframework.samples.petclinic.visits.appointment;

import java.time.LocalDateTime;

public record AppointmentSlot(LocalDateTime start, LocalDateTime end) {
}
