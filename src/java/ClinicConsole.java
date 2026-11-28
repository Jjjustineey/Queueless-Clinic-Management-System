
 import java.util.Scanner;

public class ClinicConsole {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("====================================");
        System.out.println("         QUEUELESS CLINIC");
        System.out.println("====================================");
        System.out.println("[1] Run Main Clinic Flow");
        System.out.println("[2] Exit");
        System.out.print("Enter your choice: ");

        int choice = scanner.nextInt();

        if (choice == 1) {
            ClinicWorkflow.run();
        } else if (choice == 2) {
            System.out.println("Program terminated.");
        } else {
            System.out.println("Invalid choice.");
        }

        scanner.close();
    }
}
