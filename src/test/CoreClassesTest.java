import java.time.LocalDate;
import java.time.LocalTime;

/** Lightweight checks for every core model class, including rejected input. */
public class CoreClassesTest {
    private static int passed;
    private static int failed;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void expectThrows(String name, Class<? extends Throwable> expected,
                                     Runnable action) {
        try {
            action.run();
            check(name, false);
        } catch (Throwable actual) {
            check(name, expected.isInstance(actual));
        }
    }

    private static User user() {
        return new User("u1", "Alex Morgan", "alex_m", "alex@example.com",
                "09170000000", "hash", UserRole.PATIENT);
    }

    private static Patient patient() {
        return new Patient("p1", "Alex Morgan", "alex_m", "alex@example.com",
                "09170000000", "hash", "Patient details", "Regular Patient");
    }

    private static ClinicSchedule schedule() {
        return new ClinicSchedule();
    }

    private static Appointment appointment(Patient patient) {
        return new Appointment("a1", patient,
                new MedicalService("s1", "General Consultation", "General care"),
                schedule(), LocalDate.of(2026, 11, 30), LocalTime.of(8, 0), false);
    }

    private static void testUser() {
        User user = user();
        check("User stores fields and matches email, username, and mobile",
                user.getId().equals("u1")
                && user.getFullName().equals("Alex Morgan")
                && user.getRole() == UserRole.PATIENT
                && user.matchesLoginIdentity("ALEX@example.com")
                && user.matchesLoginIdentity("ALEX_M")
                && user.matchesLoginIdentity("09170000000")
                && !user.matchesLoginIdentity(null));
        expectThrows("User rejects blank required fields", IllegalArgumentException.class,
                () -> new User(" ", "Alex Morgan", "alex_m", "alex@example.com",
                        "09170000000", "hash", UserRole.PATIENT));
        expectThrows("User rejects null role", NullPointerException.class,
                () -> new User("u1", "Alex Morgan", "alex_m", "alex@example.com",
                        "09170000000", "hash", null));
    }

    private static void testUserRole() {
        check("UserRole defines all supported roles",
                UserRole.valueOf("PATIENT") == UserRole.PATIENT
                && UserRole.valueOf("CLINIC_STAFF") == UserRole.CLINIC_STAFF
                && UserRole.valueOf("DOCTOR") == UserRole.DOCTOR);
        expectThrows("UserRole rejects unknown role names", IllegalArgumentException.class,
                () -> UserRole.valueOf("VISITOR"));
    }

    private static void testMedicalService() {
        MedicalService service = new MedicalService(" s1 ", " Dental ", " Checkup ");
        check("MedicalService stores trimmed details",
                service.getServiceId().equals("s1")
                && service.getName().equals("Dental")
                && service.getDescription().equals("Checkup"));
        expectThrows("MedicalService rejects blank names", IllegalArgumentException.class,
                () -> new MedicalService("s1", " ", "Checkup"));
    }

    private static void testClinicSchedule() {
        ClinicSchedule schedule = schedule();
        check("ClinicSchedule accepts configured weekday slot",
                schedule.isAvailable(LocalDate.of(2026, 11, 30), LocalTime.of(8, 0))
                && !schedule.isAvailable(LocalDate.of(2026, 11, 28), LocalTime.of(8, 0))
                && !schedule.isAvailable(LocalDate.of(2026, 11, 30), LocalTime.of(11, 0)));
        expectThrows("ClinicSchedule rejects invalid clinic hours", IllegalArgumentException.class,
                () -> new ClinicSchedule("Weekdays", LocalTime.of(17, 0),
                        LocalTime.of(8, 0), "11:00-13:00", "09:00"));
        expectThrows("ClinicSchedule rejects slots during lunch", IllegalArgumentException.class,
                () -> new ClinicSchedule("Weekdays", LocalTime.of(8, 0),
                        LocalTime.of(17, 0), "11:00-13:00", "11:00"));
        expectThrows("ClinicSchedule rejects malformed lunch break", IllegalArgumentException.class,
                () -> new ClinicSchedule("Weekdays", LocalTime.of(8, 0),
                        LocalTime.of(17, 0), "lunch", "09:00"));
    }

    private static void testAppointment() {
        Patient patient = patient();
        Appointment appointment = appointment(patient);
        check("Appointment starts upcoming at a valid slot and can be assigned a doctor",
                appointment.getStatus() == AppointmentStatus.UPCOMING
                && appointment.getPatient() == patient
                && appointment.getDate().equals(LocalDate.of(2026, 11, 30)));
        Doctor doctor = new Doctor("d1", "Dr. Lee", "lee", "lee@example.com",
                "09170000001", "hash", "staff1", "doctor1");
        appointment.assignDoctor(doctor);
        check("Appointment retains assigned doctor", appointment.getDoctor() == doctor);
        expectThrows("Appointment rejects blank ID", IllegalArgumentException.class,
                () -> new Appointment(" ", LocalDate.of(2026, 11, 30),
                        LocalTime.of(8, 0), AppointmentStatus.UPCOMING, false));
        expectThrows("Appointment rejects null date", NullPointerException.class,
                () -> new Appointment("a2", null, LocalTime.of(8, 0),
                        AppointmentStatus.UPCOMING, false));
        expectThrows("Appointment rejects unavailable schedule slot", IllegalArgumentException.class,
                () -> new Appointment("a3", patient, null, schedule(),
                        LocalDate.of(2026, 11, 30), LocalTime.of(11, 0), false));
        expectThrows("Appointment rejects null doctor", NullPointerException.class,
                () -> appointment.assignDoctor(null));

        Appointment withQueue = appointment(patient);
        QueueEntry queue = new QueueEntry(1, 0, QueueStatus.WAITING, "0 min", patient, withQueue);
        withQueue.assignQueueEntry(queue);
        expectThrows("Appointment rejects completion before queue is checking",
                IllegalStateException.class, withQueue::complete);
        queue.startChecking();
        withQueue.complete();
        check("Appointment completion updates appointment and queue",
                withQueue.getStatus() == AppointmentStatus.COMPLETED
                && queue.getStatus() == QueueStatus.COMPLETED);
        expectThrows("Appointment rejects reassigning an active booking",
                IllegalStateException.class, appointment(patient)::reassign);
    }

    private static void testAppointmentStatus() {
        check("AppointmentStatus defines lifecycle values",
                AppointmentStatus.values().length == 5
                && AppointmentStatus.valueOf("UPCOMING") == AppointmentStatus.UPCOMING);
        expectThrows("AppointmentStatus rejects unknown values", IllegalArgumentException.class,
                () -> AppointmentStatus.valueOf("INVALID"));
    }

    private static void testPatient() {
        Patient patient = patient();
        patient.registerInformation("Updated details", "PWD");
        PatientRecord record = new PatientRecord("r1", "Record details");
        patient.attachPatientRecord(record);
        Appointment appointment = appointment(patient);
        patient.bookAppointment(appointment);
        check("Patient updates profile and links record and appointment",
                patient.getPatientInfo().equals("Updated details")
                && patient.getCategory().equals("PWD")
                && patient.getPatientRecord() == record
                && patient.viewHistory().size() == 1);
        expectThrows("Patient rejects blank profile information", IllegalArgumentException.class,
                () -> patient.registerInformation(" ", "PWD"));
        expectThrows("Patient rejects null record", NullPointerException.class,
                () -> patient.attachPatientRecord(null));
        expectThrows("Patient rejects appointment owned by someone else", IllegalArgumentException.class,
                () -> patient.cancelEligibleAppointment(appointment(new Patient("p2", "Sam Lee",
                        "sam_lee", "sam@example.com", "09170000002", "hash", "Info", "Regular"))));
    }

    private static void testPatientRecord() {
        PatientRecord record = new PatientRecord("r1", "Initial details");
        record.updatePatientInformation("Updated details");
        Appointment appointment = appointment(patient());
        record.addAppointment(appointment);
        record.addAppointment(appointment);
        check("PatientRecord updates details and avoids duplicate history entries",
                record.getPatientInformation().equals("Updated details")
                && record.getAppointments().size() == 1
                && record.getAppointmentHistory().contains("a1"));
        expectThrows("PatientRecord rejects blank information", IllegalArgumentException.class,
                () -> record.updatePatientInformation(" "));
        expectThrows("PatientRecord rejects null appointment", NullPointerException.class,
                () -> record.addAppointment(null));
    }

    private static void testClinicStaff() {
        ClinicStaff staff = new ClinicStaff("st1", "Pat Staff", "pat", "pat@example.com",
                "09170000003", "hash", "staff-id");
        check("ClinicStaff has staff role and ID",
                staff.getRole() == UserRole.CLINIC_STAFF && staff.getStaffId().equals("staff-id"));
        expectThrows("ClinicStaff rejects blank staff ID", IllegalArgumentException.class,
                () -> new ClinicStaff("st2", "Pat Staff", "pat2", "pat2@example.com",
                        "09170000004", "hash", " "));
        expectThrows("ClinicStaff rejects patient role", IllegalArgumentException.class,
                () -> new ClinicStaff("st3", "Pat Staff", "pat3", "pat3@example.com",
                        "09170000005", "hash", "staff3", UserRole.PATIENT));
    }

    private static void testDoctor() {
        Doctor doctor = new Doctor("d1", "Dr. Lee", "lee", "lee@example.com",
                "09170000001", "hash", "staff1", "doctor1");
        check("Doctor has doctor role and identifiers",
                doctor.getRole() == UserRole.DOCTOR
                && doctor.getStaffId().equals("staff1")
                && doctor.getDoctorId().equals("doctor1"));
        expectThrows("Doctor rejects blank doctor ID", IllegalArgumentException.class,
                () -> new Doctor("d2", "Dr. Lee", "lee2", "lee2@example.com",
                        "09170000006", "hash", "staff2", " "));
        expectThrows("Doctor rejects blank staff ID", IllegalArgumentException.class,
                () -> new Doctor("d3", "Dr. Lee", "lee3", "lee3@example.com",
                        "09170000007", "hash", " ", "doctor3"));
    }

    private static void testQueueEntry() {
        QueueEntry queue = new QueueEntry(1, 0, QueueStatus.WAITING, "15 min");
        queue.updateQueuePosition(2);
        queue.updateWaitingEstimate("30 min");
        queue.startChecking();
        queue.complete();
        check("QueueEntry updates position, estimate, and valid lifecycle",
                queue.getQueueNumber() == 1 && queue.getQueuePosition() == 2
                && queue.getWaitingEstimate().equals("30 min")
                && queue.getStatus() == QueueStatus.COMPLETED);
        expectThrows("QueueEntry rejects nonpositive queue number", IllegalArgumentException.class,
                () -> new QueueEntry(0, 0, QueueStatus.WAITING, "0 min"));
        expectThrows("QueueEntry rejects negative position", IllegalArgumentException.class,
                () -> new QueueEntry(1, -1, QueueStatus.WAITING, "0 min"));
        expectThrows("QueueEntry rejects blank estimate", IllegalArgumentException.class,
                () -> new QueueEntry(1, 0, QueueStatus.WAITING, " "));
        expectThrows("QueueEntry rejects negative position updates", IllegalArgumentException.class,
                () -> queue.updateQueuePosition(-1));
        expectThrows("QueueEntry rejects blank estimate updates", IllegalArgumentException.class,
                () -> queue.updateWaitingEstimate(" "));
        expectThrows("QueueEntry rejects invalid state transition", IllegalStateException.class,
                () -> new QueueEntry(2, 0, QueueStatus.WAITING, "0 min").complete());
    }

    private static void testQueueStatus() {
        check("QueueStatus defines all supported states",
                QueueStatus.values().length == 5
                && QueueStatus.valueOf("WAITING") == QueueStatus.WAITING);
        expectThrows("QueueStatus rejects unknown values", IllegalArgumentException.class,
                () -> QueueStatus.valueOf("INVALID"));
    }

    public static void main(String[] args) {
        testUser();
        testUserRole();
        testMedicalService();
        testClinicSchedule();
        testAppointment();
        testAppointmentStatus();
        testPatient();
        testPatientRecord();
        testClinicStaff();
        testDoctor();
        testQueueEntry();
        testQueueStatus();
        System.out.println("\nCore class tests: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }
}
