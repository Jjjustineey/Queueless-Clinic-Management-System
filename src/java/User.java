import java.util.Objects;

public class User {
    private final String id;
    private final String fullName;
    private final String username;
    private final String email;
    private final String mobile;
    private final String passwordHash;
    private final UserRole role;

    public User(String id, String fullName, String username, String email,
                String mobile, String passwordHash, UserRole role) {
        this.id = required(id, "id");
        this.fullName = required(fullName, "fullName");
        this.username = required(username, "username");
        this.email = required(email, "email");
        this.mobile = required(mobile, "mobile");
        this.passwordHash = required(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }

    public boolean matchesLoginIdentity(String identity) {
        if (identity == null) return false;
        return email.equalsIgnoreCase(identity)
                || username.equalsIgnoreCase(identity)
                || mobile.equals(identity);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
