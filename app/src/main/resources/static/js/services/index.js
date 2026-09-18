import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

window.adminLoginHandler = async function () {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  const admin = { username, password };

  try {
    const response = await fetch(ADMIN_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(admin),
    });

    if (response.ok) {
      const data = await response.json();
      localStorage.setItem("token", data.token);
      selectRole("admin");
    } else {
      alert("Invalid credentials!");
    }
  } catch (error) {
    alert("An error occurred. Please try again.");
  }
};

window.doctorLoginHandler = async function () {
  const email = document.getElementById("email").value;
  const password = document.getElementById("password").value;

  const doctor = { identifier: email, password };

  try {
    const response = await fetch(DOCTOR_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    if (response.ok) {
      const data = await response.json();
      localStorage.setItem("token", data.token);
      selectRole("doctor");
    } else {
      alert("Invalid credentials!");
    }
  } catch (error) {
    console.error(error);
    alert("An error occurred. Please try again.");
  }
};
