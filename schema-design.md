# Smart Clinic Management System – Schema Design

## MySQL Database Design

MySQL stores all core structured and relational data in the Smart Clinic system. Each table maps directly to a JPA `@Entity` model class in the Spring Boot application.

---

### Table: admin

Stores administrator credentials used for secure portal login and platform management.

| Column     | Type         | Constraints                  |
|------------|--------------|------------------------------|
| id         | BIGINT       | Primary Key, Auto Increment  |
| username   | VARCHAR(50)  | NOT NULL, UNIQUE             |
| password   | VARCHAR(255) | NOT NULL                     |

**Notes:**
- Password is stored as a BCrypt hash; never exposed in API responses (`@JsonProperty WRITE_ONLY`).
- Only one admin role exists in this system; no foreign key dependencies.

---

### Table: doctors

Stores doctor profile information, credentials, and available consultation time slots.

| Column     | Type         | Constraints                          |
|------------|--------------|--------------------------------------|
| id         | BIGINT       | Primary Key, Auto Increment          |
| name       | VARCHAR(100) | NOT NULL                             |
| specialty  | VARCHAR(50)  | NOT NULL                             |
| email      | VARCHAR(100) | NOT NULL, UNIQUE                     |
| password   | VARCHAR(255) | NOT NULL                             |
| phone      | CHAR(10)     | NOT NULL, must match `^[0-9]{10}$`   |

**Notes:**
- `email` is unique to prevent duplicate accounts.
- `password` is write-only; not serialized in JSON responses.
- `phone` is validated to be exactly 10 digits.

---

### Table: doctor_available_times

Stores the available time slots for each doctor as a separate collection table (from `@ElementCollection` on `List<String> availableTimes`).

| Column            | Type         | Constraints                        |
|-------------------|--------------|------------------------------------|
| doctor_id         | BIGINT       | Foreign Key → doctors(id), NOT NULL|
| available_times   | VARCHAR(20)  | e.g., "09:00-10:00"                |

**Notes:**
- Each row represents one available one-hour slot for a doctor.
- When a doctor is deleted, their available time slots are also removed (cascading delete).
- Appointment booking logic uses this table to validate slot availability before confirming a booking.

---

### Table: patients

Stores patient registration details used for login and appointment booking.

| Column   | Type         | Constraints                        |
|----------|--------------|------------------------------------|
| id       | BIGINT       | Primary Key, Auto Increment        |
| name     | VARCHAR(100) | NOT NULL                           |
| email    | VARCHAR(100) | NOT NULL, UNIQUE                   |
| password | VARCHAR(255) | NOT NULL                           |
| phone    | CHAR(10)     | NOT NULL, must match `^[0-9]{10}$` |
| address  | VARCHAR(255) | NOT NULL                           |

**Notes:**
- `email` is unique to prevent duplicate patient accounts.
- `phone` is validated to be exactly 10 digits.
- If a patient is deleted, their associated appointments should be cancelled or archived first to preserve appointment history integrity.

---

### Table: appointments

Stores all scheduled consultations, linking a patient to a doctor at a specific time.

| Column           | Type     | Constraints                              |
|------------------|----------|------------------------------------------|
| id               | BIGINT   | Primary Key, Auto Increment              |
| doctor_id        | BIGINT   | Foreign Key → doctors(id), NOT NULL      |
| patient_id       | BIGINT   | Foreign Key → patients(id), NOT NULL     |
| appointment_time | DATETIME | NOT NULL, must be a future datetime      |
| status           | INT      | NOT NULL, 0 = Scheduled, 1 = Completed   |

**Notes:**
- `appointment_time` is validated with `@Future` to prevent past bookings.
- Each appointment is fixed at 1 hour; end time is computed as `appointment_time + 1 hour` (transient, not stored).
- A doctor should not have two appointments with overlapping times — enforced at the service layer.
- On doctor deletion, existing appointments must be handled (cancelled or reassigned) before removal.
- Appointment date and time-only views are derived via `getAppointmentDate()` and `getAppointmentTimeOnly()` helper methods on the entity.

---

## MongoDB Collection Design

MongoDB stores flexible, document-based data that does not fit neatly into relational tables. In this system, prescription records vary in structure (different medications, dosages, optional notes) and benefit from schema flexibility and rapid evolution.

---

### Collection: prescriptions

Stores prescription records issued by doctors during appointments. Each document is linked to a MySQL appointment by `appointmentId`.

```json
{
  "_id": "ObjectId('64abc123def456')",
  "patientName": "Jane Doe",
  "appointmentId": 42,
  "medication": "Amoxicillin",
  "dosage": "500mg",
  "doctorNotes": "Take 1 capsule every 8 hours with food. Complete the full 7-day course.",
  "refillCount": 1,
  "issuedAt": "2025-09-15T10:30:00Z",
  "tags": ["antibiotic", "short-term"],
  "pharmacy": {
    "name": "HealthPlus Pharmacy",
    "location": "123 Main Street, Springfield"
  },
  "followUp": {
    "required": true,
    "recommendedDate": "2025-09-22"
  }
}
```

**Field Descriptions:**

| Field         | Type     | Description                                                    |
|---------------|----------|----------------------------------------------------------------|
| _id           | ObjectId | Auto-generated unique identifier for the prescription          |
| patientName   | String   | Full name of the patient (NOT NULL, 3–100 chars)               |
| appointmentId | Long     | Reference to the MySQL `appointments.id` (NOT NULL)            |
| medication    | String   | Name of the prescribed medication (NOT NULL, 3–100 chars)      |
| dosage        | String   | Dosage instructions (NOT NULL)                                 |
| doctorNotes   | String   | Optional free-form notes from the doctor (max 200 chars)       |
| refillCount   | Integer  | Number of allowed prescription refills (optional, default 0)  |
| issuedAt      | DateTime | Timestamp when the prescription was created                    |
| tags          | Array    | Optional labels for categorization (e.g., "antibiotic")        |
| pharmacy      | Object   | Embedded pharmacy details: name and location (optional)        |
| followUp      | Object   | Embedded follow-up info: required (boolean) and recommendedDate|

**Design Decisions:**
- Only `patientName` is embedded rather than the full patient object, to avoid data duplication with MySQL.
- `appointmentId` acts as a cross-database reference to the MySQL `appointments` table.
- Nested `pharmacy` and `followUp` objects are embedded because they are tightly scoped to this prescription and do not need to be queried independently.
- `tags` array allows flexible categorization without requiring schema migrations as new drug types are introduced.
- If the schema needs to evolve (e.g., adding `sideEffects` or `insuranceClaim`), new fields can be added to new documents without altering existing ones — a key advantage of MongoDB over rigid SQL schemas.
