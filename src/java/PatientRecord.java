import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PatientRecord {
    private final String recordId;
    private String patientInformation;
    private String appointmentHistory;
    private final List<Appointment> appointments = new ArrayList<>();

    public PatientRecord(String recordId, String patientInformation) {
        this.recordId = required(recordId, "recordId");
        this.patientInformation = required(patientInformation, "patientInformation");
        this.appointmentHistory = "";
    }

    public String getRecordId() { return recordId; }
    public String getPatientInformation() { return patientInformation; }
    public String getAppointmentHistory() { return appointmentHistory; }
    public List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }

    public void updatePatientInformation(String patientInformation) {
        this.patientInformation = required(patientInformation, "patientInformation");
    }

    public void addAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment must not be null");
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
            appointmentHistory = appointments.stream()
                    .map(a -> a.getAppointmentId() + ":" + a.getStatus())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
