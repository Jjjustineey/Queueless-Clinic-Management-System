import java.util.Objects;

public class ClinicStaff extends User {
    private final String staffId;

    public ClinicStaff(String id, String fullName, String username, String email,
                       String mobile, String passwordHash, String staffId) {
        this(id, fullName, username, email, mobile, passwordHash, staffId, UserRole.CLINIC_STAFF);
    }

    protected ClinicStaff(String id, String fullName, String username, String email,
                          String mobile, String passwordHash, String staffId,
                          UserRole role) {
        super(id, fullName, username, email, mobile, passwordHash,
                Objects.requireNonNull(role, "role must not be null"));
        if (role != UserRole.CLINIC_STAFF && role != UserRole.DOCTOR) {
            throw new IllegalArgumentException("ClinicStaff role must be CLINIC_STAFF or DOCTOR");
        }
        this.staffId = required(staffId, "staffId");
    }

    public String getStaffId() { return staffId; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
