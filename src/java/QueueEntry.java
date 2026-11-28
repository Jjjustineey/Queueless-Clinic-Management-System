import java.util.Objects;

public class QueueEntry {

    private final int queueNumber;
    private int queuePosition;
    private QueueStatus status;
    private String waitingEstimate;
    private final Patient patient;
    private final Appointment appointment;

    public QueueEntry(int queueNumber, int queuePosition,
                      QueueStatus status, String waitingEstimate) {
        this(queueNumber, queuePosition, status, waitingEstimate, null, null);
    }

    public QueueEntry(int queueNumber, int queuePosition,
                      QueueStatus status, String waitingEstimate,
                      Patient patient, Appointment appointment) {

        // Validate queue number
        if (queueNumber <= 0) {
            throw new IllegalArgumentException(
                "Queue number must be positive."
            );
        }

        // Validate queue position
        if (queuePosition < 0) {
            throw new IllegalArgumentException(
                "Queue position must not be negative."
            );
        }

        // Validate queue status
        this.status = Objects.requireNonNull(
            status, "Queue status must not be null."
        );

        // Validate waiting estimate
        this.waitingEstimate = required(
            waitingEstimate, "Waiting estimate"
        );

        // Validate patient and appointment relationship
        if (appointment != null && patient == null) {
            throw new IllegalArgumentException(
                "A patient is required when an appointment is provided."
            );
        }

        if (appointment != null
                && appointment.getPatient() != null
                && appointment.getPatient() != patient) {
            throw new IllegalArgumentException(
                "Appointment belongs to another patient."
            );
        }

        this.queueNumber = queueNumber;
        this.queuePosition = queuePosition;
        this.patient = patient;
        this.appointment = appointment;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public String getWaitingEstimate() {
        return waitingEstimate;
    }

    public Patient getPatient() {
        return patient;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    // Update queue position
    public void updateQueuePosition(int queuePosition) {
        if (queuePosition < 0) {
            throw new IllegalArgumentException(
                "Queue position must not be negative."
            );
        }

        this.queuePosition = queuePosition;
    }

    // Update waiting estimate
    public void updateWaitingEstimate(String waitingEstimate) {
        this.waitingEstimate = required(
            waitingEstimate, "Waiting estimate"
        );
    }

    // Update queue status
    public void updateStatus(QueueStatus newStatus) {

        Objects.requireNonNull(
            newStatus, "New queue status must not be null."
        );

        if (!isAllowedTransition(status, newStatus)) {
            throw new IllegalStateException(
                "Invalid queue status transition from "
                + status + " to " + newStatus
            );
        }

        this.status = newStatus;
    }

    // Queue state-changing methods
    public void startChecking() {
        updateStatus(QueueStatus.CHECKING);
    }

    public void complete() {
        updateStatus(QueueStatus.COMPLETED);
    }

    public void markNoShow() {
        updateStatus(QueueStatus.NO_SHOW);
    }

    public void cancel() {
        updateStatus(QueueStatus.CANCELLED);
    }

    // Check allowed status transitions
    private static boolean isAllowedTransition(
            QueueStatus from, QueueStatus to) {

        if (from == to) {
            return true;
        }

        return (from == QueueStatus.WAITING &&
                (to == QueueStatus.CHECKING ||
                 to == QueueStatus.NO_SHOW ||
                 to == QueueStatus.CANCELLED))

                || (from == QueueStatus.CHECKING &&
                to == QueueStatus.COMPLETED);
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
