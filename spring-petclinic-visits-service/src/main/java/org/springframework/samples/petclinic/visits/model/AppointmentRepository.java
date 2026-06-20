package org.springframework.samples.petclinic.visits.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByPetIdOrderByStartTimeAsc(int petId);

    Optional<Appointment> findByIdAndPetId(int id, int petId);

    boolean existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
        int vetId,
        AppointmentStatus status,
        LocalDateTime end,
        LocalDateTime start);

    boolean existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
        int petId,
        AppointmentStatus status,
        LocalDateTime end,
        LocalDateTime start);
}
