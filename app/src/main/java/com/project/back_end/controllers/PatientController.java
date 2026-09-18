package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    @Autowired
    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    // GET /patient/{token}
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        Map<String, Object> validation = service.validateToken(token, "patient");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validation);
        }
        return patientService.getPatientDetails(token);
    }

    // POST /patient
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        boolean isNew = service.validatePatient(patient);
        if (!isNew) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Patient with email id or phone no already exist"));
        }

        int result = patientService.createPatient(patient);
        if (result == 1) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Signup successful"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error"));
        }
    }

    // POST /patient/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    // GET /patient/{id}/{token}
    @GetMapping("/{id}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(
            @PathVariable Long id,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, "patient");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validation);
        }
        return patientService.getPatientAppointment(id, token);
    }

    // GET /patient/filter/{condition}/{name}/{token}
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {

        Map<String, Object> validation = service.validateToken(token, "patient");
        if (!validation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(validation);
        }
        return service.filterPatient(condition, name, token);
    }

}
