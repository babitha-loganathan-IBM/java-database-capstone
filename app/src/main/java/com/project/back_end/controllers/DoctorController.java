package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    @Autowired
    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // GET /doctor/availability/{user}/{doctorId}/{date}/{token}
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, user);
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validation);
        }

        List<String> availability = doctorService.getDoctorAvailability(doctorId, date);
        Map<String, Object> response = new HashMap<>();
        response.put("availability", availability);
        return ResponseEntity.ok(response);
    }

    // GET /doctor
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctorService.getDoctors());
        return ResponseEntity.ok(response);
    }

    // POST /doctor/{token}
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, "admin");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized: invalid token."));
        }

        int result = doctorService.saveDoctor(doctor);
        if (result == 1) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Doctor added to db"));
        } else if (result == -1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Doctor already exists"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    // POST /doctor/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
        return doctorService.validateDoctor(login);
    }

    // PUT /doctor/{token}
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, "admin");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized: invalid token."));
        }

        int result = doctorService.updateDoctor(doctor);
        if (result == 1) {
            return ResponseEntity.ok(Map.of("message", "Doctor updated"));
        } else if (result == -1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Doctor not found"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    // DELETE /doctor/{id}/{token}
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable long id,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, "admin");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized: invalid token."));
        }

        int result = doctorService.deleteDoctor(id);
        if (result == 1) {
            return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully"));
        } else if (result == -1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Doctor not found with id"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Some internal error occurred"));
        }
    }

    // GET /doctor/filter/{name}/{time}/{speciality}
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(
            @PathVariable String name,
            @PathVariable String time,
            @PathVariable String speciality) {

        Map<String, Object> result = service.filterDoctor(name, speciality, time);
        return ResponseEntity.ok(result);
    }

}
