# QueueLess Clinic Management System

**Course:** IT211  
**Section:** BSIT 2-3  
**Group:** 2

## Project Description

The QueueLess Clinic System is a software project designed to improve clinic operations by reducing long waiting lines through scheduled appointments, digital patient registration, and organized queue management. It aims to minimize patient waiting time, prevent overcrowding, and provide a more convenient and efficient clinic experience.

## Team Members

| Member | Role |
|---|---|
| Gicanal, Fatima G. | Project Manager |
| Gimana, Lordilyn G. | UI/UX Designer |
| Pernito, Jastine A. | Frontend Developer |
| Dioquino, Carla Jane R. | Backend Developer |
| Gidoc, Gille Ann G. | Documentation |
| Dedase, Jamaica A. | Quality Assurance |

## Requirements

- Java Development Kit (JDK) compatible with the project's Java source code
- A modern web browser
- PowerShell, Command Prompt, or the VS Code integrated terminal

## Run locally

Open PowerShell in the project folder (the folder containing **README.md**) and compile the Java files:

~~~powershell
New-Item -ItemType Directory -Force out
javac -d out src\java\*.java
~~~

Start the application:

~~~powershell
java -cp out Main
~~~

**Note:** Make sure **Main** is the correct entry-point class containing the **main()** method. If the project uses a separate Java HTTP server, start the appropriate server class instead.

To stop the application, press **Ctrl+C** in the terminal.

## Using the app

Patients can register by providing their required personal information, selecting their patient category, choosing a medical service, and scheduling an appointment or selecting a walk-in visit.

After registration, patients can monitor their queue number, queue position, and estimated waiting time. Clinic staff can also assist patients who do not have access to a personal device by registering them directly into the system.

## Features

- Register and manage patient information.
- Schedule and manage clinic appointments.
- Support walk-in and scheduled patient visits.
- Select medical services, including General Consultation, Pediatrics, Maternal Check Up, Dental Check Up, and Laboratory Services.
- Generate and manage patient queue numbers.
- Monitor queue positions and estimated waiting times.
- Prioritize senior citizens, pregnant patients, and persons with disabilities (PWD).
- Support legal guardians registering or assisting patients.
- Manage doctor schedules and clinic appointments.
- Organize clinic operations through different user roles.
- Validate inputs to prevent invalid records and transactions.

## Prototype notes

- This is an academic project developed for IT211.
- Queue positions and estimated waiting times depend on the current queue and clinic operations.
- Priority patients receive appropriate consideration in queue management.
- Patients without personal devices can request assistance from clinic staff.
- Emergency patients should immediately notify clinic staff instead of relying on the regular online queue.
- Actual deployment would require proper database management, security implementation, and user authentication.

## Project files

- **src/web/app.js**: Browser interactions and frontend functionality
- **src/web/index.html**: Web page and user interface
- **src/web/style.css**: Styling and responsive layouts
- **src/java/Appointment.java**: Appointment information and management
- **src/java/AppointmentStatus.java**: Appointment statuses
- **src/java/ClinicConsole.java**: Console-based clinic operations
- **src/java/ClinicSchedule.java**: Clinic schedules
- **src/java/ClinicStaff.java**: Clinic staff information and operations
- **src/java/ClinicWorkflow.java**: Clinic processes and workflows
- **src/java/Doctor.java**: Doctor information and management
- **src/java/Main.java**: Main application entry point
- **src/java/MedicalService.java**: Available medical services
- **src/java/Patient.java**: Patient information and management
- **src/java/PatientRecord.java**: Patient records
- **src/java/QueueEntry.java**: Individual queue entries
- **src/java/QueueStatus.java**: Queue statuses
- **src/java/User.java**: User information and account management
- **src/java/UserRole.java**: System user roles
- **src/test/ClinicSystemTest.java** and **src/test/ValidationTest.java**: Lightweight Java tests
- **docs/proposal.md**: Project proposal
- **docs/class-ownership.md**: Class ownership and member assignments
- **diagrams/**: UML diagrams and other system diagrams

## Folder structure

~~~text
QueueLess Clinic Management System/
|-- diagrams/       UML and system diagrams
|-- docs/            Proposal and class ownership
|-- out/             Compiled Java classes
|-- src/
|   |-- java/         Java classes and clinic operations
|   |-- test/         Java test classes
|   |-- web/          HTML, CSS, and JavaScript files
|-- README.md         Setup and usage guide
~~~
