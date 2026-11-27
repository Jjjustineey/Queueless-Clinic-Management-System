# QueueLess Clinic Management System

## 1. Project Overview
QueueLess Clinic is a web-based patient registration, appointment, and queue management platform for the Rural Health Unit of Bulan. It aims to reduce long physical lines, improve visibility into appointment progress, and make clinic operations easier to coordinate.

## 2. Problem Statement
Traditional clinic workflows can involve long waiting lines, crowded waiting areas, manual scheduling, uncertainty about a patient's turn, and difficulty tracking cancellations and missed appointments. Patients may need to repeatedly ask staff for queue updates, while clinic personnel must coordinate records and queues manually.

## 3. Proposed Solution
The proposed platform provides a public clinic information website, patient portal, and staff/doctor workspace. Patients can create accounts, register their details, book appointments, monitor their queue, and review their appointment history. Staff can monitor the daily workload, call the next patient, move a patient into Checking, complete consultations, record No-Show cases, add walk-ins, and reassign released appointment slots.

## 4. Objectives
1. Reduce unnecessary physical waiting and overcrowding.
2. Allow patients to register and book clinic appointments online.
3. Provide queue number and appointment status visibility.
4. Help staff and doctors coordinate consultations and patient records.
5. Provide controlled handling of walk-in patients and released appointment slots.
6. Present clinic services, vision, mission, contact details, and operating hours in one accessible portal.

## 5. Target Users
- *Patients:* register, book, view queue, cancel eligible appointments, and review profile/history.
- *Clinic staff:* monitor appointments, add walk-ins, search patients, and manage queue statuses.
- *Doctors / healthcare providers:* call patients, mark Checking, complete consultations, and review patient details.
- *Visitors:* browse public clinic information and create an account.

## 6. Main Modules
1. Public Information: Home, About Us, Services, Contact.
2. Authentication: patient/staff sign-up and login, logout, password reset.
3. Patient Registration: demographics and patient category.
4. Appointment Scheduling: service, weekday, time-slot validation, and double-booking prevention.
5. Patient Queue: queue number, position, estimated wait, status, refresh, and cancellation.
6. Patient Profile: personal information and appointment history.
7. Staff Dashboard: daily totals, queue overview, and recent activity.
8. Queue Management: search, Add Patient, Call Next, Checking, Complete, No-Show, and restricted Re-Assign.
9. Appointments: upcoming and historical appointment status summaries.
10. Patient Records: searchable patient directory and appointment history.

## 7. Queue and Reassignment Rules
A normal appointment begins in *Waiting*. When the doctor starts the consultation, staff changes it to *Checking*. Once finished, staff changes it to *Completed*. If a patient fails to arrive, staff may mark the appointment *No-Show*. A patient may cancel an eligible Waiting appointment, changing it to *Cancelled*.

Reassignment is not a general queue action. It is allowed only after the source appointment is *Cancelled* or *No-Show*. Staff then assigns the released slot to a walk-in patient. The system creates a new walk-in queue record and preserves the original appointment record for history. Waiting, Checking, and Completed appointments cannot be reassigned.

## 8. Clinic Schedule
The prototype enforces Monday to Friday, 8:00 AM to 5:00 PM, with lunch break from 11:00 AM to 1:00 PM. One-hour appointment starts are 8:00, 9:00, and 10:00 AM, and 1:00, 2:00, 3:00, and 4:00 PM. Weekend, lunch, and out-of-hours appointments are rejected.

## 9. Technology Architecture
- Frontend: HTML5, CSS3, JavaScript.
- Backend: Java using the built-in HTTP server.
- Storage: local JSON file in data/store.json for this runnable prototype.
- Runtime: JDK 17 or newer; compiled classes are placed in out/.
- Startup and shutdown: run.bat and stop.bat.

## 10. Security and Privacy
The prototype hashes passwords and uses role checks and session cookies. It is intended for demonstration and development, not direct production use with real patient information. A production version should use Argon2id/bcrypt, HTTPS, secure session storage, CSRF protection, database access controls, audit logging, encrypted backups, staff account approval, verified OTP delivery, privacy notices, and a formal privacy/security review.

## 11. Implementation Phases
1. Confirm clinic workflow, user roles, and requirements.
2. Implement authentication, patient registration, and appointment scheduling.
3. Implement queue tracking, staff dashboard, queue actions, and patient records.
4. Test schedule validation, duplicate booking, status transitions, and reassignment restrictions.
5. Conduct clinic user acceptance testing and security review before deployment.

## 12. Expected Benefits
- Better visibility into queue progress.
- More organized patient appointments and clinic operations.
- Less time spent asking for queue updates.
- Better tracking of cancelled, completed, and missed appointments.
- A consistent process for assigning released slots to walk-in patients.

## 13. Conclusion
QueueLess Clinic provides a practical foundation for improving patient access and clinic workflow through online appointment scheduling and queue management. Its central rule-based queue workflow supports clearer patient communication and more organized staff operations.
