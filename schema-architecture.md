# Smart Clinic Management System – Architecture Design

## Section 1: Architecture Summary

This Spring Boot application uses both MVC and REST controllers to serve different types of users and clients. Thymeleaf templates are used for the Admin and Doctor dashboards, providing server-rendered HTML pages delivered directly to the browser. REST APIs serve all other modules — including Appointments, PatientDashboard, and PatientRecord — enabling lightweight JSON-based communication for mobile apps and future frontend clients.

The application follows a three-tier architecture: a Presentation Tier (Thymeleaf views and REST API consumers), an Application Tier (Spring Boot controllers, service layer, and business logic), and a Data Tier (MySQL and MongoDB databases). All controllers route requests through a common service layer, which applies business rules and coordinates workflows before delegating to the appropriate repositories. MySQL uses Spring Data JPA entities for structured relational data (patients, doctors, appointments, and admin records), while MongoDB uses document models (annotated with `@Document`) for flexible prescription records.

This dual-database approach leverages the strengths of both relational and document-oriented storage, while Spring Boot's developer-friendly ecosystem enables easy containerization, CI/CD integration, and horizontal scaling.

---

## Section 2: Numbered Flow of Data and Control

1. **User Interface Layer** – Users access the application either through Thymeleaf-based web dashboards (AdminDashboard, DoctorDashboard) rendered server-side in the browser, or through REST API clients such as mobile apps or frontend modules (Appointments, PatientDashboard, PatientRecord) that communicate via HTTP and receive JSON responses.

2. **Controller Layer** – Each incoming request is routed to the appropriate controller based on the URL path and HTTP method. Requests for server-rendered pages are handled by Thymeleaf Controllers, which return populated HTML templates. Requests from API consumers are handled by REST Controllers, which validate the input and return JSON responses.

3. **Service Layer** – All controllers delegate business logic to the Service Layer, which is the core of the backend. This layer applies business rules, performs validations, and coordinates multi-entity workflows (e.g., verifying doctor availability before booking an appointment). It ensures a clean separation between controller logic and data access.

4. **Repository Layer** – The Service Layer communicates with the Repository Layer to perform CRUD operations. This layer contains two types of repositories: MySQL Repositories using Spring Data JPA for relational entities, and a MongoDB Repository using Spring Data MongoDB for document-based prescription records.

5. **Database Access** – Each repository interfaces directly with its underlying database engine. MySQL stores normalized relational data (users, roles, appointments) with referential constraints. MongoDB stores flexible, nested prescription documents that may vary in structure and allow rapid schema evolution.

6. **Model Binding** – Data retrieved from the database is mapped into Java model classes. MySQL data is converted into JPA entities annotated with `@Entity`, representing rows in relational tables. MongoDB data is loaded into document objects annotated with `@Document`, mapped to BSON/JSON collections. These models provide a consistent, object-oriented representation across all application layers.

7. **Response to Client** – The bound models are used to generate the final response. In MVC flows, models are passed to Thymeleaf templates and rendered as dynamic HTML for the browser. In REST flows, models (or their DTO equivalents) are serialized into JSON and returned to the client as part of an HTTP response, completing the request-response cycle.
