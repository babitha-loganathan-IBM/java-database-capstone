import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const data = await response.json();
    return data.doctors || [];
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, {
      method: "DELETE",
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message,
    };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return { success: false, message: "Failed to delete doctor." };
  }
}

export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message,
    };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return { success: false, message: "Failed to save doctor." };
  }
}

export async function filterDoctors(name, time, specialty) {
  try {
    const response = await fetch(
      `${DOCTOR_API}/filter/${name}/${time}/${specialty}`
    );
    if (response.ok) {
      const data = await response.json();
      return data;
    } else {
      console.error("Filter request failed:", response.status);
      return { doctors: [] };
    }
  } catch (error) {
    alert("Error filtering doctors. Please try again.");
    return { doctors: [] };
  }
}
