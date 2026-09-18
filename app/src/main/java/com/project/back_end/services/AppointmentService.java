package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final com.project.back_end.services.Service service;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService,
                              com.project.back_end.services.Service service) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.service = service;
    }

    // Book a new appointment; returns 1 on success, 0 on failure
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Update an existing appointment
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> existing = appointmentRepository.findById(appointment.getId());
            if (existing.isEmpty()) {
                response.put("message", "Appointment not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment current = existing.get();

            // Ensure the patient matches
            if (!current.getPatient().getId().equals(appointment.getPatient().getId())) {
                response.put("message", "Unauthorized: patient mismatch.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Validate the new appointment time against doctor availability
            int valid = service.validateAppointment(
                    appointment.getAppointmentTime(),
                    appointment.getDoctor().getId());

            if (valid == -1) {
                response.put("message", "Doctor not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else if (valid == 0) {
                response.put("message", "Doctor is not available at the requested time.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            appointmentRepository.save(appointment);
            response.put("message", "Appointment updated successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Error updating appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Cancel (delete) an appointment by ID, verifying the patient via token
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> optional = appointmentRepository.findById(id);
            if (optional.isEmpty()) {
                response.put("message", "Appointment not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment appointment = optional.get();
            String email = tokenService.extractEmail(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null || !appointment.getPatient().getId().equals(patient.getId())) {
                response.put("message", "Unauthorized: you can only cancel your own appointments.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            appointmentRepository.delete(appointment);
            response.put("message", "Appointment cancelled successfully.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Error cancelling appointment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Retrieve appointments for a doctor on a given date, optionally filtered by patient name
    @Transactional
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            Doctor doctor = doctorRepository.findByEmail(email);

            if (doctor == null) {
                result.put("appointments", new ArrayList<>());
                return result;
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);

            List<Appointment> appointments;

            if (pname != null && !pname.equals("null") && !pname.isBlank()) {
                appointments = appointmentRepository
                        .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname, start, end);
            } else {
                appointments = appointmentRepository
                        .findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            }

            List<AppointmentDTO> dtos = new ArrayList<>();
            for (Appointment a : appointments) {
                Patient p = a.getPatient();
                dtos.add(new AppointmentDTO(
                        a.getId(),
                        doctor.getId(),
                        doctor.getName(),
                        p.getId(),
                        p.getName(),
                        p.getEmail(),
                        p.getPhone(),
                        p.getAddress(),
                        a.getAppointmentTime(),
                        a.getStatus()
                ));
            }

            result.put("appointments", dtos);
        } catch (Exception e) {
            result.put("appointments", new ArrayList<>());
        }
        return result;
    }

    // Change the status of an appointment
    @Transactional
    public void changeStatus(int status, long id) {
        Optional<Appointment> optional = appointmentRepository.findById(id);
        optional.ifPresent(appointment -> {
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
        });
    }

}
