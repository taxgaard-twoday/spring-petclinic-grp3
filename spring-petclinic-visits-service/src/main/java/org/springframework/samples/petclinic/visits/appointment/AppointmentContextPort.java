package org.springframework.samples.petclinic.visits.appointment;

public interface AppointmentContextPort {

    void validate(int ownerId, int petId, int vetId);
}
