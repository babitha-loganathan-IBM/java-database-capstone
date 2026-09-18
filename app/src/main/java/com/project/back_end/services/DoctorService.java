package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // Returns available (unbooked) time slots for a doctor on a given date
    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> optional = doctorRepository.findById(doctorId);
        if (optional.isEmpty()) return new ArrayList<>();

        Doctor doctor = optional.get();
        List<String> allSlots = new ArrayList<>(doctor.getAvailableTimes());

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        List<Appointment> booked = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        List<String> bookedSlots = booked.stream()
                .map(a -> {
                    LocalTime t = a.getAppointmentTime().toLocalTime();
                    return String.format("%02d:00-%02d:00", t.getHour(), t.getHour() + 1);
                })
                .collect(Collectors.toList());

        allSlots.removeAll(bookedSlots);
        return allSlots;
    }

    // Save a new doctor; returns 1 on success, -1 if email exists, 0 on error
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Update an existing doctor; returns 1 on success, -1 if not found, 0 on error
    public int updateDoctor(Doctor doctor) {
        try {
            if (!doctorRepository.existsById(doctor.getId())) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Retrieve all doctors
    @Transactional
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    // Delete a doctor and all their appointments; returns 1 on success, -1 if not found, 0 on error
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) return -1;
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Validate doctor login; returns token on success or error message
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
            if (doctor == null) {
                response.put("message", "Doctor not found.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            if (!doctor.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid password.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            String token = tokenService.generateToken(doctor.getEmail());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Internal server error.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Find doctors by partial name match
    @Transactional
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctorRepository.findByNameLike(name));
        return result;
    }

    // Filter by name + specialty + AM/PM
    @Transactional
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    // Filter by name + AM/PM
    @Transactional
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    // Filter by name + specialty
    @Transactional
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specilty));
        return result;
    }

    // Filter by specialty + AM/PM
    @Transactional
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty);
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filterDoctorByTime(doctors, amOrPm));
        return result;
    }

    // Filter by specialty only
    @Transactional
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctorRepository.findBySpecialtyIgnoreCase(specilty));
        return result;
    }

    // Filter all doctors by AM/PM availability
    @Transactional
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> all = doctorRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filterDoctorByTime(all, amOrPm));
        return result;
    }

    // Private helper: filters a doctor list by whether any available slot is AM or PM
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        return doctors.stream()
                .filter(d -> d.getAvailableTimes() != null && d.getAvailableTimes().stream()
                        .anyMatch(slot -> {
                            try {
                                int hour = Integer.parseInt(slot.split(":")[0]);
                                if ("AM".equalsIgnoreCase(amOrPm)) return hour < 12;
                                if ("PM".equalsIgnoreCase(amOrPm)) return hour >= 12;
                            } catch (Exception ignored) {}
                            return false;
                        }))
                .collect(Collectors.toList());
    }

}
