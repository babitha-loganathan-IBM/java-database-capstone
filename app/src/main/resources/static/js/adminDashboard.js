import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// Load doctor cards and wire filter/search listeners after DOM is ready
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();

  // #addDocBtn is injected by header.js — use event delegation on document
  document.addEventListener("click", (e) => {
    if (e.target && e.target.id === "addDocBtn") {
      window.openModal && window.openModal("addDoctor");
    }
  });

  document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
  document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
  document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);
});

async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";
    doctors.forEach((doctor) => {
      const card = createDoctorCard(doctor);
      contentDiv.appendChild(card);
    });
  } catch (error) {
    console.error("Error loading doctor cards:", error);
  }
}

async function filterDoctorsOnChange() {
  const name = document.getElementById("searchBar").value.trim() || null;
  const time = document.getElementById("filterTime").value || null;
  const specialty = document.getElementById("filterSpecialty").value || null;

  try {
    const data = await filterDoctors(name, time, specialty);
    const doctors = data.doctors || [];
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";

    if (doctors.length === 0) {
      contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
      return;
    }

    renderDoctorCards(doctors);
  } catch (error) {
    alert("Error filtering doctors. Please try again.");
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";
  doctors.forEach((doctor) => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

window.adminAddDoctor = async function () {
  const name = document.getElementById("doctorName").value.trim();
  const email = document.getElementById("doctorEmail").value.trim();
  const phone = document.getElementById("doctorPhone").value.trim();
  const password = document.getElementById("doctorPassword").value.trim();
  const specialization = document.getElementById("specialization").value;

  const availabilityCheckboxes = document.querySelectorAll(
    "input[name='availability']:checked"
  );
  const availableTimes = Array.from(availabilityCheckboxes).map((cb) => cb.value);

  const token = localStorage.getItem("token");
  if (!token) {
    alert("You are not authenticated. Please log in again.");
    return;
  }

  const doctor = { name, email, phone, password, specialty: specialization, availableTimes };

  const result = await saveDoctor(doctor, token);

  if (result.success) {
    alert(result.message || "Doctor added successfully!");
    document.getElementById("modal").style.display = "none";
    await loadDoctorCards();
  } else {
    alert(result.message || "Failed to add doctor. Please try again.");
  }
};
