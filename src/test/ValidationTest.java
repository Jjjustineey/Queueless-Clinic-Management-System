/** Lightweight tests for account field validation. */
public class ValidationTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static boolean isRejected(String fullName, String username,
                                      String email, String mobile,
                                      String password, String confirmation) {
        try {
            Main.validateAccountFields(fullName, username, email, mobile,
                    password, confirmation);
            return false;
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    public static void main(String[] args) {
        try {
            Main.validateAccountFields("Alex Morgan", "alex_m", "alex@example.com",
                    "+1 555 123 4567", "secret1", "secret1");
            check("Valid account fields accepted", true);
        } catch (IllegalArgumentException exception) {
            check("Valid account fields accepted", false);
        }

        check("Blank name rejected", isRejected("", "alex_m", "alex@example.com", "5551234567", "secret1", "secret1"));
        check("Short name rejected", isRejected("A", "alex_m", "alex@example.com", "5551234567", "secret1", "secret1"));
        check("Invalid name characters rejected", isRejected("Alex123", "alex_m", "alex@example.com", "5551234567", "secret1", "secret1"));
        check("Short username rejected", isRejected("Alex Morgan", "ab", "alex@example.com", "5551234567", "secret1", "secret1"));
        check("Invalid username characters rejected", isRejected("Alex Morgan", "alex!", "alex@example.com", "5551234567", "secret1", "secret1"));
        check("Invalid email rejected", isRejected("Alex Morgan", "alex_m", "not-an-email", "5551234567", "secret1", "secret1"));
        check("Invalid mobile characters rejected", isRejected("Alex Morgan", "alex_m", "alex@example.com", "555-ABC-123", "secret1", "secret1"));
        check("Short password rejected", isRejected("Alex Morgan", "alex_m", "alex@example.com", "5551234567", "12345", "12345"));
        check("Password mismatch rejected", isRejected("Alex Morgan", "alex_m", "alex@example.com", "5551234567", "secret1", "secret2"));

        System.out.println("\nTest summary: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }
}
