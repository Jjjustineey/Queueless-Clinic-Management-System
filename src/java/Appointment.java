import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class Appointment {

    private final String appointmentId;
    private final LocalDate date;
    private final LocalTime time;
    private AppointmentStatus status;
    private final boolean walkIn;
    private final Patient patient;
    private final MedicalService medicalService;
    private final ClinicSchedule clinicSchedule;
    private Doctor doctor;
    private QueueEntry queueEntry;

    // Constructor for basic appointments
    public Appointment(String appointmentId, LocalDate date,
                       LocalTime time, AppointmentStatus status,
                       boolean walkIn) {

        this(appointmentId, null, null, null, date, time, status, walkIn);
    }

    // Constructor with default appointment status
    public Appointment(String appointmentId, Patient patient,
                       MedicalService medicalService,
                       ClinicSchedule clinicSchedule,
                       LocalDate date, LocalTime time,
                       boolean walkIn) {

        this(appointmentId, patient, medicalService, clinicSchedule,
                date, time, AppointmentStatus.UPCOMING, walkIn);
    }

    // Main constructor with validation
    public Appointment(String appointmentId, Patient patient,
                       MedicalService medicalService,
                       ClinicSchedule clinicSchedule,
                       LocalDate date, LocalTime time,
                       AppointmentStatus status, boolean walkIn) {

        this.appointmentId = required(appointmentId, "appointmentId");

        this.date = Objects.requireNonNull(
                date, "Appointment date must not be null."
        );

        this.time = Objects.requireNonNull(
                time, "Appointment time must not be null."
        );

        this.status = Objects.requireNonNull(
                status, "Appointment status must not be null."
        );

        this.walkIn = walkIn;
        this.patient = patient;
        this.medicalService = medicalService;
        this.clinicSchedule = clinicSchedule;

        // Validate clinic schedule if provided
        if (clinicSchedule != null) {
            clinicSchedule.validateAppointmentSlot(date, time);
        }
    }

    // Getters
    public String getAppointmentId() {
        return appointmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public boolean isWalkIn() {
        return walkIn;
    }

    public Patient getPatient() {
        return patient;
    }

    public MedicalService getMedicalService() {
        return medicalService;
    }

    public ClinicSchedule getClinicSchedule() {
        return clinicSchedule;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public QueueEntry getQueueEntry() {
        return queueEntry;
    }

    // Book appointment
    public void book() {
        if (status != AppointmentStatus.UPCOMING) {
            throw new IllegalStateException(
                    "Only upcoming appointments can be booked."
            );
        }
    }

    // Update appointment status
    public void updateStatus(AppointmentStatus newStatus) {

        Objects.requireNonNull(
                newStatus, "New appointment status must not be null."
        );

        if (newStatus == AppointmentStatus.CANCELLED) {
            cancel();
            return;
        }

        if (newStatus == AppointmentStatus.NO_SHOW) {
            markNoShow();
            return;
        }

        if (newStatus == AppointmentStatus.COMPLETED) {
            complete();
            return;
        }

        if (newStatus == AppointmentStatus.RE_ASSIGNED) {
            reassign();
            return;
        }

        if (!isAllowedTransition(status, newStatus)) {
            throw new IllegalStateException(
                    "Invalid appointment status transition from "
                    + status + " to " + newStatus
            );
        }

        status = newStatus;
    }

    // Cancel appointment
    public void cancel() {

        if (!isAllowedTransition(status, AppointmentStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Invalid appointment status transition."
            );
        }

        validateQueueState(
                QueueStatus.WAITING,
                "Only waiting appointments can be cancelled."
        );

        if (queueEntry != null) {
            queueEntry.cancel();
        }

        status = AppointmentStatus.CANCELLED;
    }

    // Complete appointment
    public void complete() {

        // Validate appointment status first
        if (!isAllowedTransition(status, AppointmentStatus.COMPLETED)) {
            throw new IllegalStateException(
                    "Invalid appointment status transition."
            );
        }

        // Validate queue status
        validateQueueState(
                QueueStatus.CHECKING,
                "Only checking appointments can be completed."
        );

        // Update queue entry
        if (queueEntry != null) {
            queueEntry.complete();
        }

        // Update appointment status
        status = AppointmentStatus.COMPLETED;
    }

    // Mark appointment as no-show
    public void markNoShow() {

        if (!isAllowedTransition(status, AppointmentStatus.NO_SHOW)) {
            throw new IllegalStateException(
                    "Invalid appointment status transition."
            );
        }

        validateQueueState(
                QueueStatus.WAITING,
                "Only waiting appointments can be marked no-show."
        );

        if (queueEntry != null) {
            queueEntry.markNoShow();
        }

        status = AppointmentStatus.NO_SHOW;
    }

    // Reassign appointment
    public void reassign() {

        if (status != AppointmentStatus.CANCELLED
                && status != AppointmentStatus.NO_SHOW) {

            throw new IllegalStateException(
                    "Only cancelled or no-show appointments can be reassigned."
            );
        }

        status = AppointmentStatus.RE_ASSIGNED;
    }

    // Assign doctor
    public void assignDoctor(Doctor doctor) {

        Objects.requireNonNull(
                doctor, "Doctor must not be null."
        );

        this.doctor = doctor;
    }

    // Assign queue entry
    public void assignQueueEntry(QueueEntry queueEntry) {

        Objects.requireNonNull(
                queueEntry, "Queue entry must not be null."
        );

        if (this.queueEntry != null && this.queueEntry != queueEntry) {
            throw new IllegalStateException(
                    "Queue entry is already assigned."
            );
        }

        // Validate patient relationship when both are available
        if (patient != null && queueEntry.getPatient() != null
                && queueEntry.getPatient() != patient) {

            throw new IllegalArgumentException(
                    "Queue entry belongs to another patient."
            );
        }

        if (queueEntry.getAppointment() != null
                && queueEntry.getAppointment() != this) {

            throw new IllegalArgumentException(
                    "Queue entry belongs to another appointment."
            );
        }

        this.queueEntry = queueEntry;
    }

    // Validate queue status
    private void validateQueueState(
            QueueStatus requiredStatus, String message) {

        if (queueEntry != null
                && queueEntry.getStatus() != requiredStatus) {

            throw new IllegalStateException(message);
        }
    }

    // Check allowed appointment status transitions
    private static boolean isAllowedTransition(
            AppointmentStatus from, AppointmentStatus to) {

        if (from == to) {
            return true;
        }

        return from == AppointmentStatus.UPCOMING
                && (to == AppointmentStatus.COMPLETED
                || to == AppointmentStatus.CANCELLED
                || to == AppointmentStatus.NO_SHOW);
    }

    // Validate required text
    private static String required(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank."
            );
        }

        return value.trim();
    }
}