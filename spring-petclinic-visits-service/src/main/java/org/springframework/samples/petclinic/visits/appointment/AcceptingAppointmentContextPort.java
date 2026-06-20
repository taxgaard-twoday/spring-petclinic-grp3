package org.springframework.samples.petclinic.visits.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
class AcceptingAppointmentContextPort implements AppointmentContextPort {

    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(16, 0);

    @Override
    public void validate(int ownerId, int petId, int vetId) {
        // Real customer/vet validation is added in a later integration slice.
    }

    @Override
    public BookingContext bookingContext(int ownerId, int petId, int vetId) {
        return new BookingContext(List.of(
            workingHour(DayOfWeek.MONDAY),
            workingHour(DayOfWeek.TUESDAY),
            workingHour(DayOfWeek.WEDNESDAY),
            workingHour(DayOfWeek.THURSDAY),
            workingHour(DayOfWeek.FRIDAY)
        ));
    }

    private VetWorkingHour workingHour(DayOfWeek dayOfWeek) {
        return new VetWorkingHour(dayOfWeek, DEFAULT_START, DEFAULT_END);
    }
}
