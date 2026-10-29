package com.attendance.manager.controller;

import com.attendance.manager.service.QrAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QrAttendanceController {

    @Autowired
    private QrAttendanceService qrAttendanceService;

    // Endpoint 1: Teacher Screen fetches live 10-second token
    @GetMapping("/teacher/subjects/{subjectId}/qr-token")
    public ResponseEntity<?> getLiveQrToken(@PathVariable Long subjectId) {
        String token = qrAttendanceService.generateQrToken(subjectId);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // Endpoint 2: Student submits scanned QR token
    @PostMapping("/student/scan-qr")
    public ResponseEntity<?> scanQrCode(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Long studentId = Long.parseLong(payload.get("studentId").toString());
            Long subjectId = Long.parseLong(payload.get("subjectId").toString());
            String token = payload.get("token").toString();
            
            String clientIp = request.getRemoteAddr();
            Double latitude = payload.get("latitude") != null ? Double.parseDouble(payload.get("latitude").toString()) : null;
            Double longitude = payload.get("longitude") != null ? Double.parseDouble(payload.get("longitude").toString()) : null;

            qrAttendanceService.scanAndMarkAttendance(studentId, subjectId, token, clientIp, latitude, longitude);
            return ResponseEntity.ok(Map.of("message", "Attendance marked PRESENT successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error marking attendance: " + e.getMessage()));
        }
    }
}
