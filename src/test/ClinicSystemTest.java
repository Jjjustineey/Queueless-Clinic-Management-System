
/** Lightweight tests for the QueueLess Clinic prototype (no external test library needed).
 * Compile with Main.java and run this class to see the checks.
 */
public class ClinicSystemTest {
    private static int passed = 0, failed = 0;
    private static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("PASS: " + name); }
        else { failed++; System.out.println("FAIL: " + name); }
    }
    public static void main(String[] args) {
        // Weekday/date checks
        check("Monday date accepted for a slot", Main.validSlot("2026-11-30", "08:00"));
        check("Friday date accepted for a slot", Main.validSlot("2026-12-04", "16:00"));
        check("Saturday rejected", !Main.validSlot("2026-11-28", "08:00"));
        check("Sunday rejected", !Main.validSlot("2026-11-29", "08:00"));
        check("Malformed date rejected", !Main.validSlot("not-a-date", "08:00"));

        // Clinic schedule checks: only listed hourly start times are permitted.
        String[] allowed = {"08:00", "09:00", "10:00", "13:00", "14:00", "15:00", "16:00"};
        for (String time : allowed) check("Allowed appointment time " + time, Main.validSlot("2026-11-30", time));
        check("11:00 lunch slot rejected", !Main.validSlot("2026-11-30", "11:00"));
        check("12:00 rejected", !Main.validSlot("2026-11-30", "12:00"));
        check("12:30 lunch time rejected", !Main.validSlot("2026-11-30", "12:30"));
        check("17:00 outside appointment start times rejected", !Main.validSlot("2026-11-30", "17:00"));
        check("Minutes not on an hourly slot rejected", !Main.validSlot("2026-11-30", "08:30"));

        // Password hashes use a random salt, so verify passwords instead of
        // expecting repeated hashes to have the same text.
        java.util.Map<String, Object> user = Main.obj(
            "password", Main.hash("test-password")
        );
        check("Correct password verifies", Main.verifyPassword(user, "test-password"));
        check("Different password is rejected", !Main.verifyPassword(user, "other-password"));
        check("Hash is not plaintext", !user.get("password").equals("test-password"));

        System.out.println("\nTest summary: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }
}

