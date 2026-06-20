package org.springframework.samples.petclinic.visits.appointment;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.visits.model.Appointment;
import org.springframework.samples.petclinic.visits.model.AppointmentRepository;
import org.springframework.samples.petclinic.visits.model.AppointmentStatus;
import org.springframework.samples.petclinic.visits.web.BadRequestException;
import org.springframework.samples.petclinic.visits.web.ConflictException;
import org.springframework.samples.petclinic.visits.web.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {

    private static final int OWNER_ID = 10;
    private static final int PET_ID = 7;
    private static final int VET_ID = 1;
    private static final LocalDateTime VALID_START = LocalDateTime.of(2026, 6, 22, 9, 0);

    private AppointmentRepository appointmentRepository;
    private RecordingAppointmentContextPort appointmentContextPort;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        appointmentContextPort = new RecordingAppointmentContextPort();
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T08:00:00Z"), ZoneId.of("UTC"));
        appointmentService = new AppointmentService(appointmentRepository, appointmentContextPort, clock);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateValidAppointmentAndComputeEnd() {
        Appointment appointment = appointmentService.create(OWNER_ID, PET_ID, VET_ID, VALID_START);

        assertThat(appointment.getPetId()).isEqualTo(PET_ID);
        assertThat(appointment.getVetId()).isEqualTo(VET_ID);
        assertThat(appointment.getStartTime()).isEqualTo(VALID_START);
        assertThat(appointment.getEndTime()).isEqualTo(VALID_START.plusMinutes(15));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointmentContextPort.validationCount).isEqualTo(1);
        assertThat(appointmentContextPort.lastOwnerId).isEqualTo(OWNER_ID);
        assertThat(appointmentContextPort.lastPetId).isEqualTo(PET_ID);
        assertThat(appointmentContextPort.lastVetId).isEqualTo(VET_ID);
    }

    @Test
    void shouldReturnAvailableSlotsInsideVetWorkingHours() {
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "09:00", "10:00"));

        List<AppointmentSlot> slots = appointmentService.availableSlots(
            OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22));

        assertThat(slots).extracting(AppointmentSlot::start).containsExactly(
            LocalDateTime.of(2026, 6, 22, 9, 0),
            LocalDateTime.of(2026, 6, 22, 9, 15),
            LocalDateTime.of(2026, 6, 22, 9, 30),
            LocalDateTime.of(2026, 6, 22, 9, 45)
        );
        assertThat(slots).extracting(AppointmentSlot::end).last().isEqualTo(LocalDateTime.of(2026, 6, 22, 10, 0));
    }

    @Test
    void shouldExcludeSlotsOutsideVetWorkingHours() {
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "10:00", "10:30"));

        List<AppointmentSlot> slots = appointmentService.availableSlots(
            OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22));

        assertThat(slots).extracting(AppointmentSlot::start).containsExactly(
            LocalDateTime.of(2026, 6, 22, 10, 0),
            LocalDateTime.of(2026, 6, 22, 10, 15)
        );
    }

    @Test
    void shouldExcludeWeekendSlots() {
        assertThat(appointmentService.availableSlots(OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 27)))
            .isEmpty();
    }

    @Test
    void shouldExcludeSlotsInsideLeadTime() {
        useClock("2026-06-22T08:00:00Z");

        assertThat(appointmentService.availableSlots(OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22)))
            .isEmpty();
    }

    @Test
    void shouldExcludeSlotsBeyondThreeMonthHorizon() {
        assertThat(appointmentService.availableSlots(OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 9, 21)))
            .isEmpty();
    }

    @Test
    void shouldExcludeSlotsAlreadyBookedBySameVet() {
        LocalDateTime bookedStart = LocalDateTime.of(2026, 6, 22, 9, 15);
        when(appointmentRepository.existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            VET_ID, AppointmentStatus.SCHEDULED, bookedStart.plusMinutes(15), bookedStart))
            .thenReturn(true);
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "09:00", "10:00"));

        List<AppointmentSlot> slots = appointmentService.availableSlots(
            OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22));

        assertThat(slots).extracting(AppointmentSlot::start).containsExactly(
            LocalDateTime.of(2026, 6, 22, 9, 0),
            LocalDateTime.of(2026, 6, 22, 9, 30),
            LocalDateTime.of(2026, 6, 22, 9, 45)
        );
    }

    @Test
    void shouldExcludeSlotsAlreadyBookedBySamePetWithDifferentVet() {
        LocalDateTime bookedStart = LocalDateTime.of(2026, 6, 22, 9, 30);
        when(appointmentRepository.existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            PET_ID, AppointmentStatus.SCHEDULED, bookedStart.plusMinutes(15), bookedStart))
            .thenReturn(true);
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "09:00", "10:00"));

        List<AppointmentSlot> slots = appointmentService.availableSlots(
            OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22));

        assertThat(slots).extracting(AppointmentSlot::start).containsExactly(
            LocalDateTime.of(2026, 6, 22, 9, 0),
            LocalDateTime.of(2026, 6, 22, 9, 15),
            LocalDateTime.of(2026, 6, 22, 9, 45)
        );
    }

    @Test
    void shouldAllowSlotsBlockedOnlyByCancelledAppointments() {
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "09:00", "09:15"));

        List<AppointmentSlot> slots = appointmentService.availableSlots(
            OWNER_ID, PET_ID, VET_ID, LocalDate.of(2026, 6, 22));

        assertThat(slots).extracting(AppointmentSlot::start).containsExactly(LocalDateTime.of(2026, 6, 22, 9, 0));
        verify(appointmentRepository).existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            VET_ID, AppointmentStatus.SCHEDULED, LocalDateTime.of(2026, 6, 22, 9, 15),
            LocalDateTime.of(2026, 6, 22, 9, 0));
    }

    @Test
    void shouldRejectPastAppointment() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 19, 9, 0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment cannot be in the past");
    }

    @Test
    void shouldRejectAppointmentWithLessThanTwentyFourHoursLeadTime() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 21, 7, 45)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment must be booked at least 24 hours in advance");
    }

    @Test
    void shouldRejectAppointmentMoreThanThreeMonthsAhead() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 9, 21, 9, 0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment cannot be more than 3 months in the future");
    }

    @Test
    void shouldRejectWeekendAppointment() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 27, 9, 0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment must be on a weekday");
    }

    @Test
    void shouldRejectAppointmentOutsideClinicHours() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 22, 16, 0)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment must be within clinic opening hours");
    }

    @Test
    void shouldRejectAppointmentOffFifteenMinuteBoundary() {
        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 22, 9, 10)))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment must start on a 15-minute boundary");
    }

    @Test
    void shouldRejectAppointmentOutsideVetWorkingHours() {
        appointmentContextPort.vetWorkingHours = List.of(workingHour(DayOfWeek.MONDAY, "10:00", "11:00"));

        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, VALID_START))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment must be within vet working hours");
    }

    @Test
    void shouldRejectOverlappingAppointmentForSameVet() {
        when(appointmentRepository.existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            VET_ID, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START))
            .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, VET_ID, VALID_START))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Vet already has an appointment in this slot");
    }

    @Test
    void shouldRejectOverlappingAppointmentForSamePetEvenWhenVetIsDifferent() {
        int differentVetId = 2;
        when(appointmentRepository.existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            PET_ID, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START))
            .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(OWNER_ID, PET_ID, differentVetId, VALID_START))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Pet already has an appointment in this slot");
    }

    @Test
    void shouldAllowSameSlotForDifferentVetOnlyWhenPetIsAlsoDifferent() {
        Appointment appointment = appointmentService.create(OWNER_ID, 8, 2, VALID_START);

        assertThat(appointment.getPetId()).isEqualTo(8);
        assertThat(appointment.getVetId()).isEqualTo(2);
        verify(appointmentRepository).existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            2, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START);
        verify(appointmentRepository).existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            8, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START);
    }

    @Test
    void shouldAllowBookingWhenExistingAppointmentInSlotIsCancelled() {
        Appointment appointment = appointmentService.create(OWNER_ID, PET_ID, VET_ID, VALID_START);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).existsByVetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            VET_ID, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START);
        verify(appointmentRepository).existsByPetIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            PET_ID, AppointmentStatus.SCHEDULED, VALID_START.plusMinutes(15), VALID_START);
    }

    @Test
    void shouldCancelEligibleAppointment() {
        Appointment appointment = appointment(5, PET_ID, VET_ID, VALID_START);
        when(appointmentRepository.findByIdAndPetId(5, PET_ID)).thenReturn(Optional.of(appointment));

        Appointment cancelled = appointmentService.cancel(PET_ID, 5);

        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldRejectCancellationInsideTwentyFourHourWindow() {
        Appointment appointment = appointment(5, PET_ID, VET_ID, LocalDateTime.of(2026, 6, 21, 7, 45));
        when(appointmentRepository.findByIdAndPetId(5, PET_ID)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancel(PET_ID, 5))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Appointment can only be cancelled at least 24 hours before it starts");
    }

    @Test
    void shouldRejectCancellationForMissingAppointment() {
        when(appointmentRepository.findByIdAndPetId(5, PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancel(PET_ID, 5))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Appointment 5 not found");
    }

    private Appointment appointment(int id, int petId, int vetId, LocalDateTime start) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPetId(petId);
        appointment.setVetId(vetId);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(15));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    private void useClock(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
        appointmentService = new AppointmentService(appointmentRepository, appointmentContextPort, clock);
    }

    private AppointmentContextPort.VetWorkingHour workingHour(DayOfWeek dayOfWeek, String start, String end) {
        return new AppointmentContextPort.VetWorkingHour(dayOfWeek, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static final class RecordingAppointmentContextPort implements AppointmentContextPort {

        private int validationCount;
        private int lastOwnerId;
        private int lastPetId;
        private int lastVetId;
        private List<VetWorkingHour> vetWorkingHours = List.of(
            new VetWorkingHour(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(16, 0)),
            new VetWorkingHour(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(16, 0)),
            new VetWorkingHour(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(16, 0)),
            new VetWorkingHour(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(16, 0)),
            new VetWorkingHour(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(16, 0))
        );

        @Override
        public void validate(int ownerId, int petId, int vetId) {
            validationCount++;
            lastOwnerId = ownerId;
            lastPetId = petId;
            lastVetId = vetId;
        }

        @Override
        public BookingContext bookingContext(int ownerId, int petId, int vetId) {
            return new BookingContext(vetWorkingHours);
        }
    }
}
