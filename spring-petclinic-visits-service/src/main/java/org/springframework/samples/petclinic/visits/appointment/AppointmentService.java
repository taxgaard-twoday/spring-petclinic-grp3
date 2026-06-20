package org.springframework.samples.petclinic.visits.appointment;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.samples.petclinic.visits.model.Appointment;
import org.springframework.samples.petclinic.visits.model.AppointmentRepository;
import org.springframework.samples.petclinic.visits.model.AppointmentStatus;
import org.springframework.samples.petclinic.visits.web.BadRequestException;
import org.springframework.samples.petclinic.visits.web.ConflictException;
import org.springframework.samples.petclinic.visits.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final int APPOINTMENT_MINUTES = 15;
    private static final LocalTime CLINIC_OPENS = LocalTime.of(9, 0);
    private static final LocalTime LAST_START_TIME = LocalTime.of(15, 45);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentContextPort appointmentContextPort;
    private final Clock clock;

    AppointmentService(
        AppointmentRepository appointmentRepository,
        AppointmentContextPort appointmentContextPort,
        Clock clock) {

        this.appointmentRepository = appointmentRepository;
        this.appointmentContextPort = appointmentContextPort;
        this.clock = clock;
    }

    public List<Appointment> findForPet(int petId) {
        return appointmentRepository.findByPetIdOrderByStartTimeAsc(petId);
    }

    @Transactional
    public Appointment create(int ownerId, int petId, int vetId, LocalDateTime start) {
        appointmentContextPort.validate(ownerId, petId, vetId);
        validateStart(start);

        LocalDateTime end = start.plusMinutes(APPOINTMENT_MINUTES);
        rejectOverlappingAppointments(petId, vetId, start, end);

        Appointment appointment = new Appointment();
        appointment.setPetId(petId);
        appointment.setVetId(vetId);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancel(int petId, int appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndPetId(appointmentId, petId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment " + appointmentId + " not found"));

        LocalDateTime now = LocalDateTime.now(clock);
        if (appointment.getStartTime().isBefore(now.plusHours(24))) {
            throw new BadRequestException("Appointment can only be cancelled at least 24 hours before it starts");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appointment);
    }

    private void validateStart(LocalDateTime start) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (start.isBefore(now)) {
            throw new BadRequestException("Appointment cannot be in the past");
        }
        if (start.isBefore(now.plusHours(24))) {
            throw new BadRequestException("Appointment must be booked at least 24 hours in advance");
        }
        if (start.isAfter(now.plusMonths(3))) {
            throw new BadRequestException("Appointment cannot be more than 3 months in the future");
        }
        if (start.getDayOfWeek() == DayOfWeek.SATURDAY || start.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Appointment must be on a weekday");
        }
        if (start.toLocalTime().isBefore(CLINIC_OPENS) || start.toLocalTime().isAfter(LAST_START_TIME)) {
            throw new BadRequestException("Appointment must be within clinic opening hours");
        }
        if (start.getMinute() % APPOINTMENT_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new BadRequestException("Appointment must start on a 15-minute boundary");
        }
    }

    private void rejectOverlappingAppointments(int petId, int vetId, LocalDateTime start, LocalDateTime end) {
        if (appointmentRepository.existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            vetId, AppointmentStatus.SCHEDULED, end, start)) {

            throw new ConflictException("Vet already has an appointment in this slot");
        }

        if (appointmentRepository.existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            petId, AppointmentStatus.SCHEDULED, end, start)) {

            throw new ConflictException("Pet already has an appointment in this slot");
        }
    }
}
