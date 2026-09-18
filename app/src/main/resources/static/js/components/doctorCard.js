import { showBookingOverlay } from "../pages/loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

export function createDoctorCard(doctor) {
  // Main card container
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  // Current user role
  const role = localStorage.getItem("userRole");

  // Doctor info section
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name;

  const specialization = document.createElement("p");
  specialization.textContent = `Specialization: ${doctor.specialization}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${doctor.email}`;

  const availability = document.createElement("p");
  const times = Array.isArray(doctor.availableTimes)
    ? doctor.availableTimes.join(", ")
    : doctor.availableTimes || "";
  availability.textContent = `Available: ${times}`;

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  // Button container
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  // === ADMIN ROLE ACTIONS ===
  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";

    removeBtn.addEventListener("click", async () => {
      const confirmed = confirm(`Are you sure you want to delete Dr. ${doctor.name}?`);
      if (!confirmed) return;

      const token = localStorage.getItem("token");
      const result = await deleteDoctor(doctor.id, token);

      if (result) {
        alert(`Dr. ${doctor.name} has been removed.`);
        card.remove();
      } else {
        alert("Failed to delete doctor. Please try again.");
      }
    });

    actionsDiv.appendChild(removeBtn);

  // === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";

    bookNow.addEventListener("click", () => {
      alert("Patient needs to login first.");
    });

    actionsDiv.appendChild(bookNow);

  // === LOGGED-IN PATIENT ROLE ACTIONS ===
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";

    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        return;
      }
      const patientData = await getPatientData(token);
      showBookingOverlay(e, doctor, patientData);
    });

    actionsDiv.appendChild(bookNow);
  }

  // Final assembly
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}
