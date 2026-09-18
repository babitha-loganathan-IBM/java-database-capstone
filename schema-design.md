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

---

## MySQL Stored Procedures

Stored procedures encapsulate reusable SQL logic for reporting and operational tasks. They are called directly from the MySQL CLI or a database client.

---

### Procedure: GetDailyAppointmentReportByDoctor

Generates a daily appointment report grouped by doctor. For a given date, it returns every appointment with the doctor's name, appointment time, status, and the patient's name and phone number. This is useful for daily operational reviews in the clinic.

#### Definition

```sql
DELIMITER $

CREATE PROCEDURE GetDailyAppointmentReportByDoctor(
    IN report_date DATE
)
BEGIN
    SELECT 
        d.name AS doctor_name,
        a.appointment_time,
        a.status,
        p.name AS patient_name,
        p.phone AS patient_phone
    FROM 
        appointment a
    JOIN 
        doctor d ON a.doctor_id = d.id
    JOIN 
        patient p ON a.patient_id = p.id
    WHERE 
        DATE(a.appointment_time) = report_date
    ORDER BY 
        d.name, a.appointment_time;
END$

DELIMITER ;
```

#### Usage

```sql
CALL GetDailyAppointmentReportByDoctor('2025-04-15');
```

#### Output Columns

| Column          | Source Table  | Description                                      |
|-----------------|---------------|--------------------------------------------------|
| `doctor_name`   | `doctor.name` | Full name of the doctor for that appointment     |
| `appointment_time` | `appointment.appointment_time` | Scheduled date and time of the appointment |
| `status`        | `appointment.status` | `0` = Scheduled, `1` = Completed          |
| `patient_name`  | `patient.name` | Full name of the patient                        |
| `patient_phone` | `patient.phone` | Patient's 10-digit contact number              |

#### Notes
- The `IN` parameter `report_date` accepts a `DATE` value (format: `YYYY-MM-DD`).
- Results are ordered first by `doctor_name` (alphabetically), then by `appointment_time` (chronologically) — grouping all of a doctor's appointments together for easy reading.
- `DATE(a.appointment_time)` extracts just the date portion from the `DATETIME` column to match against `report_date`.
- Joins the `appointment`, `doctor`, and `patient` tables — all stored in MySQL and mapped to JPA entities [`Appointment.java`](app/src/main/java/com/project/back_end/models/Appointment.java), [`Doctor.java`](app/src/main/java/com/project/back_end/models/Doctor.java), and [`Patient.java`](app/src/main/java/com/project/back_end/models/Patient.java).
- The complete terminal output from each procedure execution should be saved for assignment submission.

---

### Procedure: GetDoctorWithMostPatientsByMonth

Finds the doctor who saw the most patients in a given month and year. Returns the `doctor_id` and the count of patients seen. Useful for monthly performance reviews and workload analysis.

#### Definition

```sql
DELIMITER $

CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(
    IN input_month INT, 
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id, 
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        MONTH(appointment_time) = input_month 
        AND YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END $

DELIMITER ;
```

#### Usage

```sql
CALL GetDoctorWithMostPatientsByMonth(4, 2025);
```

#### Output Columns

| Column          | Description                                              |
|-----------------|----------------------------------------------------------|
| `doctor_id`     | The ID of the doctor with the highest patient count      |
| `patients_seen` | Total number of appointments (patients seen) that month  |

#### Notes
- Accepts two `IN` parameters: `input_month` (1–12) and `input_year` (e.g., `2025`).
- Uses `MONTH()` and `YEAR()` functions to filter appointments within the specified period.
- `GROUP BY doctor_id` aggregates appointment counts per doctor; `ORDER BY patients_seen DESC LIMIT 1` returns only the top result.
- Returns only `doctor_id` — join with the `doctor` table to retrieve the doctor's name if needed.

---

### Procedure: GetDoctorWithMostPatientsByYear

Finds the doctor who saw the most patients across an entire year. Returns the `doctor_id` and total patients seen. Useful for annual performance reviews and resource planning.

#### Definition

```sql
DELIMITER $

CREATE PROCEDURE GetDoctorWithMostPatientsByYear(
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id, 
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END $

DELIMITER ;
```

#### Usage

```sql
CALL GetDoctorWithMostPatientsByYear(2025);
```

#### Output Columns

| Column          | Description                                              |
|-----------------|----------------------------------------------------------|
| `doctor_id`     | The ID of the doctor with the highest patient count      |
| `patients_seen` | Total number of appointments (patients seen) that year   |

#### Notes
- Accepts one `IN` parameter: `input_year` (e.g., `2025`).
- Uses `YEAR()` to filter all appointments within the specified year.
- `GROUP BY doctor_id` aggregates counts per doctor; `ORDER BY patients_seen DESC LIMIT 1` returns only the busiest doctor.
- Identical in structure to `GetDoctorWithMostPatientsByMonth` but scoped to a full year instead of a single month.
- Returns only `doctor_id` — join with the `doctor` table to retrieve the doctor's name if needed.
