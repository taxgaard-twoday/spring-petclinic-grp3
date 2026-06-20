package org.springframework.samples.petclinic.visits.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentContextPort {

    void validate(int ownerId, int petId, int vetId);

    BookingContext bookingContext(int ownerId, int petId, int vetId);

    record BookingContext(List<VetWorkingHour> vetWorkingHours) {
    }

    record VetWorkingHour(DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
    }
}
