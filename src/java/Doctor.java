public class Doctor extends ClinicStaff {
    private final String doctorId;

    public Doctor(String id, String fullName, String username, String email,
                  String mobile, String passwordHash, String staffId,
                  String doctorId) {
        super(id, fullName, username, email, mobile, passwordHash,
                staffId, UserRole.DOCTOR);

        this.doctorId = required(doctorId, "doctorId");
    }

    public String getDoctorId() {
        return doctorId;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
