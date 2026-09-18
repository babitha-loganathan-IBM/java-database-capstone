import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

// Global variables
const tableBody = document.getElementById("patientTableBody");
const today = new Date().toISOString().split("T")[0];
let selectedDate = today;
const token = localStorage.getItem("token");
let patientName = null;

// Search bar — filter by patient name
document.getElementById("searchBar").addEventListener("input", (e) => {
  const value = e.target.value.trim();
  patientName = value !== "" ? value : "null";
  loadAppointments();
});

// "Today" button — reset date to today
document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = today;
  document.getElementById("datePicker").value = today;
  loadAppointments();
});

// Date picker — update selected date
document.getElementById("datePicker").addEventListener("change", (e) => {
  selectedDate = e.target.value;
  loadAppointments();
});

async function loadAppointments() {
  try {
    const data = await getAllAppointments(selectedDate, patientName || "null", token);
    tableBody.innerHTML = "";

    const appointments = data.appointments || data || [];

    if (!appointments || appointments.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="5">No Appointments found for today.</td>
        </tr>`;
      return;
    }

    appointments.forEach((appointment) => {
      const patient = {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail,
      };
      const row = createPatientRow(patient, appointment.id, appointment.doctorId);
      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML = `
      <tr>
        <td colspan="5">Error loading appointments. Try again later.</td>
      </tr>`;
  }
}

// Initial render on page load
document.addEventListener("DOMContentLoaded", () => {
  if (typeof renderContent === "function") renderContent();
  loadAppointments();
});
