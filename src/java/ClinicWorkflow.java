
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class ClinicWorkflow {

    public static void run() {

        System.out.println("\n===== CLINIC MAIN FLOW =====");

        // 1. Create patient
        Patient patient = new Patient(
            "P001",
            "Juan Dela Cruz",
            "juan",
            "juan@example.com",
            "09123456789",
            "demo-password-hash",
            "20 years old",
            "Regular Patient"
        );

        System.out.println("\nPatient registered: " + patient.getFullName());

        // 2. Select medical service
        MedicalService service = new MedicalService(
            "MS001",
            "General Consultation",
            "General health consultation"
        );

        System.out.println("Selected service: " + service.getName());

        // 3. Set clinic schedule
        ClinicSchedule schedule = new ClinicSchedule();

        LocalDate appointmentDate = LocalDate.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        LocalTime appointmentTime = LocalTime.of(9, 0);

        // 4. Create and book appointment
        Appointment appointment = new Appointment(
            "A001",
            patient,
            service,
            schedule,
            appointmentDate,
            appointmentTime,
            false
        );

        appointment.book();
        patient.bookAppointment(appointment);

        System.out.println("\nAppointment booked successfully!");
        System.out.println("Date: " + appointment.getDate());
        System.out.println("Time: " + appointment.getTime());
        System.out.println("Status: " + appointment.getStatus());

        // 5. Create queue entry
        QueueEntry queueEntry = new QueueEntry(
            1,
            1,
            QueueStatus.WAITING,
            "1 Hour",
            patient,
            appointment
        );

        appointment.assignQueueEntry(queueEntry);

        System.out.println("\nQueue entry created!");
        System.out.println("Queue Number: " + queueEntry.getQueueNumber());
        System.out.println("Queue Position: " + queueEntry.getQueuePosition());
        System.out.println("Queue Status: " + queueEntry.getStatus());
        System.out.println("Waiting Estimate: " + queueEntry.getWaitingEstimate());

        // 6. Add patient to queue record
        patient.addQueueEntry(queueEntry);

        System.out.println("\n===== MAIN FLOW COMPLETED =====");
    }
}