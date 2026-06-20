package org.springframework.samples.petclinic.visits.appointment;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private static final LocalTime CLINIC_CLOSES = LocalTime.of(16, 0);
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

    public List<AppointmentSlot> availableSlots(int ownerId, int petId, int vetId, LocalDate date) {
        appointmentContextPort.validate(ownerId, petId, vetId);
        AppointmentContextPort.BookingContext bookingContext = appointmentContextPort.bookingContext(ownerId, petId, vetId);

        List<AppointmentSlot> slots = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(date, CLINIC_OPENS);
        LocalDateTime lastStart = LocalDateTime.of(date, LAST_START_TIME);

        while (!start.isAfter(lastStart)) {
            LocalDateTime end = start.plusMinutes(APPOINTMENT_MINUTES);
            if (isAvailableCandidate(petId, vetId, start, end, bookingContext)) {
                slots.add(new AppointmentSlot(start, end));
            }
            start = start.plusMinutes(APPOINTMENT_MINUTES);
        }

        return slots;
    }

    @Transactional
    public Appointment create(int ownerId, int petId, int vetId, LocalDateTime start) {
        appointmentContextPort.validate(ownerId, petId, vetId);
        AppointmentContextPort.BookingContext bookingContext = appointmentContextPort.bookingContext(ownerId, petId, vetId);
        validateStart(start);

        LocalDateTime end = start.plusMinutes(APPOINTMENT_MINUTES);
        validateVetWorkingHours(start, end, bookingContext);
        rejectOverlappingAppointments(petId, vetId, start, end);

        Appointment appointment = new Appointment();
        appointment.setPetId(petId);
        appointment.setVetId(vetId);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    private boolean isAvailableCandidate(
        int petId,
        int vetId,
        LocalDateTime start,
        LocalDateTime end,
        AppointmentContextPort.BookingContext bookingContext) {

        return isInsideSchedulingWindow(start)
            && isInsideVetWorkingHours(start, end, bookingContext)
            && !hasOverlappingAppointment(petId, vetId, start, end);
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
        if (!isInsideSchedulingWindow(start)) {
            validateSchedulingWindow(start);
        }
        if (start.getMinute() % APPOINTMENT_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new BadRequestException("Appointment must start on a 15-minute boundary");
        }
    }

    private boolean isInsideSchedulingWindow(LocalDateTime start) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !start.isBefore(now)
            && !start.isBefore(now.plusHours(24))
            && !start.isAfter(now.plusMonths(3))
            && start.getDayOfWeek() != DayOfWeek.SATURDAY
            && start.getDayOfWeek() != DayOfWeek.SUNDAY
            && !start.toLocalTime().isBefore(CLINIC_OPENS)
            && !start.toLocalTime().isAfter(LAST_START_TIME)
            && start.getMinute() % APPOINTMENT_MINUTES == 0
            && start.getSecond() == 0
            && start.getNano() == 0;
    }

    private void validateSchedulingWindow(LocalDateTime start) {
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
    }

    private void rejectOverlappingAppointments(int petId, int vetId, LocalDateTime start, LocalDateTime end) {
        if (hasOverlappingVetAppointment(vetId, start, end)) {

            throw new ConflictException("Vet already has an appointment in this slot");
        }

        if (hasOverlappingPetAppointment(petId, start, end)) {

            throw new ConflictException("Pet already has an appointment in this slot");
        }
    }

    private boolean hasOverlappingAppointment(int petId, int vetId, LocalDateTime start, LocalDateTime end) {
        return hasOverlappingVetAppointment(vetId, start, end) || hasOverlappingPetAppointment(petId, start, end);
    }

    private boolean hasOverlappingVetAppointment(int vetId, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            vetId, AppointmentStatus.SCHEDULED, end, start);
    }

    private boolean hasOverlappingPetAppointment(int petId, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            petId, AppointmentStatus.SCHEDULED, end, start);
    }

    private void validateVetWorkingHours(
        LocalDateTime start,
        LocalDateTime end,
        AppointmentContextPort.BookingContext bookingContext) {

        if (!isInsideVetWorkingHours(start, end, bookingContext)) {
            throw new BadRequestException("Appointment must be within vet working hours");
        }
    }

    private boolean isInsideVetWorkingHours(
        LocalDateTime start,
        LocalDateTime end,
        AppointmentContextPort.BookingContext bookingContext) {

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        return bookingContext.vetWorkingHours().stream()
            .filter(workingHour -> workingHour.dayOfWeek() == start.getDayOfWeek())
            .anyMatch(workingHour ->
                !startTime.isBefore(max(CLINIC_OPENS, workingHour.start()))
                    && !endTime.isAfter(min(CLINIC_CLOSES, workingHour.end()))
            );
    }

    private LocalTime max(LocalTime left, LocalTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalTime min(LocalTime left, LocalTime right) {
        return left.isBefore(right) ? left : right;
    }
}
