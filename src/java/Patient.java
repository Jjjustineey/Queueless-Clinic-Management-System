import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Patient extends User {
    private String patientInfo;
    private String category;
    private PatientRecord patientRecord;
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<QueueEntry> queueEntries = new ArrayList<>();

    public Patient(String id, String fullName, String username, String email,
                   String mobile, String passwordHash, String patientInfo,
                   String category) {
        super(id, fullName, username, email, mobile, passwordHash, UserRole.PATIENT);
        this.patientInfo = required(patientInfo, "patientInfo");
        this.category = required(category, "category");
    }

    public String getPatientInfo() { return patientInfo; }
    public String getCategory() { return category; }
    public PatientRecord getPatientRecord() { return patientRecord; }
    public List<Appointment> getAppointments() {
        return Collections.unmodifiableList(appointments);
    }
    public List<QueueEntry> getQueueEntries() {
        return Collections.unmodifiableList(queueEntries);
    }

    public void registerInformation(String patientInfo, String category) {
        this.patientInfo = required(patientInfo, "patientInfo");
        this.category = required(category, "category");
    }

    public void manageProfile(String patientInfo, String category) {
        registerInformation(patientInfo, category);
    }

    public void attachPatientRecord(PatientRecord record) {
        this.patientRecord = Objects.requireNonNull(record, "record must not be null");
    }

    public void bookAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment must not be null");
        if (appointment.getPatient() != null && appointment.getPatient() != this) {
            throw new IllegalArgumentException("Appointment belongs to another patient");
        }
        if (!appointments.contains(appointment)) appointments.add(appointment);
    }

    public void cancelEligibleAppointment(Appointment appointment) {
        requireOwnedAppointment(appointment);
        appointment.cancel();
    }

    public List<Appointment> viewHistory() {
        return getAppointments();
    }

   void addQueueEntry(QueueEntry entry) {
    Objects.requireNonNull(
        entry,
        "Queue entry must not be null"
    );

    if (!queueEntries.contains(entry)) {
        queueEntries.add(entry);
    }
}

    private void requireOwnedAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment must not be null");
        if (!appointments.contains(appointment)) {
            throw new IllegalArgumentException("Appointment does not belong to this patient");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
