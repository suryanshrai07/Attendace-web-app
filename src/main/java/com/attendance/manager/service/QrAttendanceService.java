package com.attendance.manager.service;

import com.attendance.manager.model.AttendanceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QrAttendanceService {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private com.attendance.manager.repository.SubjectRepository subjectRepository;

    // 1. Generate 10-Second Token for Teacher Screen
    public String generateQrToken(Long subjectId) {
        long timeBucket = System.currentTimeMillis() / 10000; // Changes every 10 seconds
        return subjectId + "-" + timeBucket;
    }

    // 2. Haversine Distance Formula (calculates distance in meters between two GPS coordinates)
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in meters
    }

    // 3. Overloaded scanAndMarkAttendance for backward compatibility
    public void scanAndMarkAttendance(Long studentId, Long subjectId, String studentToken, String clientIp) {
        scanAndMarkAttendance(studentId, subjectId, studentToken, clientIp, null, null);
    }

    // 4. Validate Student Scan & Mark Attendance with Geolocation Check
    public void scanAndMarkAttendance(Long studentId, Long subjectId, String studentToken, String clientIp, Double studentLat, Double studentLon) {
        
        System.out.println("Processing QR scan from client IP: " + clientIp + " (Lat: " + studentLat + ", Lon: " + studentLon + ")");

        // CHECK 1: Verify 10-Second TOTP Token
        long currentBucket = System.currentTimeMillis() / 10000;
        String validNow = subjectId + "-" + currentBucket;
        String validPrev = subjectId + "-" + (currentBucket - 1);

        if (!studentToken.trim().equals(validNow) && !studentToken.trim().equals(validPrev)) {
            throw new IllegalArgumentException("QR Code Expired! Please scan the live screen in class.");
        }

        // CHECK 2: Geolocation Radius Verification (if Subject has classroom GPS configured)
        com.attendance.manager.model.Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        if (subject.getLatitude() != null && subject.getLongitude() != null) {
            if (studentLat == null || studentLon == null) {
                throw new IllegalArgumentException("Classroom GPS verification is enabled. Please allow location permissions on your phone to mark attendance.");
            }

            double distance = calculateDistanceMeters(studentLat, studentLon, subject.getLatitude(), subject.getLongitude());
            double maxAllowedRadius = (subject.getAllowedRadiusMeters() != null) ? subject.getAllowedRadiusMeters() : 100.0;

            if (distance > maxAllowedRadius) {
                throw new IllegalArgumentException(String.format("Location Mismatch! You are %.0fm away from the classroom (Maximum allowed radius: %.0fm).", distance, maxAllowedRadius));
            }
        }

        // Mark Attendance in Database
        attendanceService.markSingleStudentAttendance(studentId, subjectId, AttendanceStatus.PRESENT);
    }
}
