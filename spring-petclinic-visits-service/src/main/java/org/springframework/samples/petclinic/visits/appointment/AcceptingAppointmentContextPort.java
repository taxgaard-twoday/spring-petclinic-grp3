package org.springframework.samples.petclinic.visits.appointment;

import org.springframework.stereotype.Component;

@Component
class AcceptingAppointmentContextPort implements AppointmentContextPort {

    @Override
    public void validate(int ownerId, int petId, int vetId) {
        // Real customer/vet validation is added in a later integration slice.
    }
}
