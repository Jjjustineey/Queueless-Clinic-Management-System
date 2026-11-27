QueueLess Clinic Management System — Must and Should Requirements
1. Must Requirements
These are the essential requirements that the QueueLess Clinic System must implement.

1.1 User Access and Authentication
Visitors must be able to view Home, About Us, Services, and Contact pages.
Visitors must be able to create an account and log in.
Patients and clinic staff must have separate access roles.
Users must be able to log out.
The system must support account registration using full name, username, email, mobile number, password, and confirm password.
Users must be able to log in using their email, username, or mobile number.
The system must provide password reset through a verification code.
1.2 Patient Module
Patients must be able to complete their registration.
Patients must be able to book appointments by selecting a service, date, and time.
Patients must be able to view their appointment details and queue status.
Patients must be able to view their profile and appointment history.
Patients must be able to cancel eligible appointments.
The system must display queue numbers, queue positions, waiting estimates, and appointment statuses.
1.3 Appointment Scheduling Rules
The system must allow appointments from Monday to Friday, 8:00 AM–5:00 PM.
The system must reject bookings during the 11:00 AM–1:00 PM lunch break.
The system must only allow the available one-hour appointment start times: 8 AM, 9 AM, 10 AM, 1 PM, 2 PM, 3 PM, and 4 PM.
The system must reject weekend and out-of-hours bookings.
The system must prevent double-booking of active appointment slots.
Cancelled and No-Show appointment slots may be released for reassignment.
1.4 Staff / Doctor Module
Staff must be able to access the dashboard.
Staff must be able to view patient and appointment statistics.
Staff must be able to search patient records.
Staff must be able to add walk-in patients.
Staff must be able to call the next patient.
Staff must be able to update patient queue statuses.
Staff must be able to view patient details and appointment history.
Staff must be able to manage upcoming, completed, cancelled, reassigned, and No-Show appointments.
1.5 Queue Management Rules
The system must support the status flow: Waiting → Checking → Completed.
Waiting appointments may also become No-Show.
Patients must be allowed to cancel Waiting appointments.
Reassignment must only be permitted for Cancelled or No-Show appointments.
The system must prevent reassignment of Waiting, Checking, and Completed records.
The original appointment record must be retained and marked Re-Assigned after its released slot is used.
The system must create a new walk-in appointment and queue record for the walk-in patient during reassignment.
1.6 Non-Functional Requirements
The system must have a responsive and accessible interface.
The system must use Java for the backend.
The frontend must use HTML, CSS, and JavaScript.
The system must support local JSON data persistence.
The system must support local startup and shutdown using the provided scripts.
The system must run locally at http://localhost:8080.
The system must require JDK 17 or newer.
2. Should Requirements
These are important improvements recommended for future development and production deployment.

The system should use a database instead of local JSON file storage.
Passwords should use a dedicated secure password hashing algorithm such as Argon2id or bcrypt.
The system should use HTTPS and CSRF protection.
Session storage should be hardened and should support secure session management.
Password reset verification codes should be delivered through a verified email or SMS provider.
Staff accounts should have a proper role provisioning and approval process.
The system should include audit logs.
The system should provide consent and privacy notices.
The system should support reliable data backups.
The system should undergo compliance review under applicable Philippine privacy and health-data requirements.
Queue waiting estimates should be reviewed against actual clinic workflow and priority policies.
3. Real-World Limit the System Must Enforce
The system must only accept one active appointment per available one-hour slot from Monday to Friday, between 8:00 AM and 5:00 PM, excluding the 11:00 AM–1:00 PM lunch break, and must only allow reassignment of appointments that are Cancelled or No-Show.

4. Review and Agreement
All group members have reviewed the Must and Should requirements, discussed the necessary details, and agreed on the final list. The requirements are approved by the group and will serve as the basis for the QueueLess Clinic System development.
