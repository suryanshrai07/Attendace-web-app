package com.attendance.manager.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.FileOutputStream;

public class GenerateInterviewGuidePdf {

    public static void main(String[] args) {
        String outputPath = "C:\\Users\\mahaj\\.gemini\\antigravity\\brain\\8b652e6f-8842-4c46-b05f-a4c64905f0b3\\SmartAttend_Master_Placement_Interview_Guide.pdf";
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 23, 42));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, new Color(71, 85, 105));
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13.5f, new Color(37, 99, 235));
            Font subSectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 23, 42));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(30, 41, 59));
            Font boldBody = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, new Color(15, 23, 42));
            Font codeFont = FontFactory.getFont(FontFactory.COURIER, 8.5f, new Color(15, 23, 42));

            // TITLE
            Paragraph title = new Paragraph("SmartAttend: Project Implementation & Architecture Master Guide", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("Deep Project Trade-offs ('Why X instead of Y'), Internal Execution Flows & Complete API Code", subTitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            document.add(new Paragraph("_________________________________________________________________________________", subTitleFont));
            document.add(Chunk.NEWLINE);

            // SECTION 1: ARCHITECTURAL TRADE-OFFS ("WHY X INSTEAD OF Y?")
            addSection(document, "SECTION 1: Architectural Trade-Offs ('Why X instead of Y?')", sectionHeadingFont);

            addQnA(document, "1. Why did you use Dynamic TOTP QR Codes instead of Static QR Codes or Geofencing?",
                "WHY NOT STATIC QR: Static QR codes are shared on WhatsApp in seconds, allowing absent students to scan from home.\n" +
                "WHY NOT GEOFENCING: GPS geofencing fails inside multi-story college buildings (poor satellite fix) and allows fake GPS location-spoofing apps.\n" +
                "WHY TOTP QR: TOTP calculates a 10-second token based on epoch time (currentTimeMillis / 10000). The token expires before a screenshot can be texted home.");

            addQnA(document, "2. Why did you use STOMP over WebSockets instead of HTTP Polling or Server-Sent Events (SSE)?",
                "WHY NOT HTTP POLLING: Polling makes 1000s of HTTP GET requests per minute, creating massive server CPU and network overhead.\n" +
                "WHY NOT SSE: Server-Sent Events are uni-directional (Server to Client only).\n" +
                "WHY STOMP WEBSOCKETS: WebSockets provide a persistent full-duplex TCP connection. STOMP adds pub/sub messaging topics (/topic/subject/{id}), letting the server push live scans to the teacher screen instantly.");

            addQnA(document, "3. Why did you use Stateless JWT Authentication instead of Stateful HTTP Sessions?",
                "WHY NOT SESSIONS: Stateful sessions store user state in server RAM. If you scale to 5 backend server nodes, a user logged into Node 1 gets logged out when hitting Node 2 (requires sticky sessions or Redis session replication).\n" +
                "WHY JWT: JWT is stateless. The client sends 'Authorization: Bearer <token>' in the header. Any node can cryptographically verify the signature without reading server RAM.");

            addQnA(document, "4. Why did you use Composite Unique Constraints in Database instead of checking duplicates in Java code?",
                "WHY NOT JAVA CHECK: If two requests arrive simultaneously in parallel threads, both Java checks see 'No existing record' and both insert, causing duplicate attendance.\n" +
                "WHY DATABASE CONSTRAINT: A Composite Unique Key (student_id, subject_id, date) enforces uniqueness at the database engine level, guaranteeing atomicity regardless of concurrent threads.");

            addQnA(document, "5. Why did you use OpenPDF byte-streaming instead of JasperReports or HTML templates?",
                "WHY NOT JASPERREPORTS: JasperReports has massive XML dependencies and slow compilation times.\n" +
                "WHY OPENPDF: OpenPDF dynamically generates PDF documents in memory as raw byte streams (ByteArrayInputStream) and pipes them directly to HTTP response headers (Content-Disposition: attachment), using zero disk space.");

            addQnA(document, "6. Why did you use Vanilla JavaScript instead of React or Angular for the frontend?",
                "WHY VANILLA JS: The core complexity of SmartAttend is Backend Engineering (Spring Boot, Security, WebSockets, TOTP). Using Vanilla JS with Fetch API and SockJS kept the application lightweight with zero npm dependencies and zero build bundle overhead.");

            document.add(Chunk.NEWLINE);

            // SECTION 2: INTERNAL STEP-BY-STEP EXECUTION FLOWS
            addSection(document, "SECTION 2: Internal Step-by-Step Execution Flows", sectionHeadingFont);

            addParagraph(document, "Flow A: Internal Execution When Student Scans Live QR Code", boldBody);
            addCodeBlock(document, 
                "1. Student submits scanned token (e.g. '1-170000') via POST /api/student/scan-qr\n" +
                "2. JwtRequestFilter intercepts request -> Validates JWT token signature -> Sets SecurityContext\n" +
                "3. QrAttendanceController receives request -> Calls QrAttendanceService.scanAndMarkAttendance()\n" +
                "4. QrAttendanceService calculates current epoch bucket: System.currentTimeMillis() / 10000\n" +
                "5. Checks if studentToken matches validNow OR validPrev (10-second grace window)\n" +
                "6. If valid, calls AttendanceService.markSingleStudentAttendance() wrapped in @Transactional\n" +
                "7. JPA saves/updates row in 'attendance' table in PostgreSQL/MySQL\n" +
                "8. SimpMessagingTemplate broadcasts STOMP payload to WebSocket topic: /topic/subject/1\n" +
                "9. Teacher's browser receives STOMP frame -> Triggering loadTeacherSubjectData() -> Auto-refreshing table live!", codeFont);

            addParagraph(document, "Flow B: Internal Execution of Spring Security JwtRequestFilter", boldBody);
            addCodeBlock(document, 
                "1. HTTP Request hits Tomcat -> Enters Spring Security FilterChain\n" +
                "2. JwtRequestFilter reads header: 'Authorization: Bearer <token>'\n" +
                "3. JwtUtil parses claims & checks signature using HMAC-SHA256 secret key\n" +
                "4. Checks token expiration date (expiration > currentTime)\n" +
                "5. CustomUserDetailsService loads UserDetails from Database\n" +
                "6. Creates UsernamePasswordAuthenticationToken & populates SecurityContextHolder.getContext().setAuthentication()\n" +
                "7. Calls filterChain.doFilter(request, response) to proceed to @RestController", codeFont);

            document.add(Chunk.NEWLINE);

            // SECTION 3: EXACT CODE IMPLEMENTATIONS FOR ALL APIS
            addSection(document, "SECTION 3: Exact Code Implementations (Whiteboard Ready)", sectionHeadingFont);

            addParagraph(document, "API 1: Dynamic QR Code Generator & Validation Logic (QrAttendanceService.java)", boldBody);
            addCodeBlock(document, 
                "@Service\n" +
                "public class QrAttendanceService {\n" +
                "    @Autowired private AttendanceService attendanceService;\n" +
                "\n" +
                "    // Generate 10-second Token\n" +
                "    public String generateQrToken(Long subjectId) {\n" +
                "        long timeBucket = System.currentTimeMillis() / 10000;\n" +
                "        return subjectId + \"-\" + timeBucket;\n" +
                "    }\n" +
                "\n" +
                "    // Validate Token & Mark Attendance\n" +
                "    public void scanAndMarkAttendance(Long studentId, Long subjectId, String token) {\n" +
                "        long currentBucket = System.currentTimeMillis() / 10000;\n" +
                "        String validNow = subjectId + \"-\" + currentBucket;\n" +
                "        String validPrev = subjectId + \"-\" + (currentBucket - 1);\n" +
                "\n" +
                "        if (!token.equals(validNow) && !token.equals(validPrev)) {\n" +
                "            throw new IllegalArgumentException(\"QR Code Expired! Scan live screen.\");\n" +
                "        }\n" +
                "        attendanceService.markSingleStudentAttendance(studentId, subjectId, AttendanceStatus.PRESENT);\n" +
                "    }\n" +
                "}", codeFont);

            addParagraph(document, "API 2: Mark Attendance Endpoint (TeacherController.java)", boldBody);
            addCodeBlock(document, 
                "@PostMapping(\"/subjects/{subjectId}/attendance\")\n" +
                "public ResponseEntity<?> markAttendance(\n" +
                "        @PathVariable Long subjectId,\n" +
                "        @RequestBody MarkAttendanceRequest request,\n" +
                "        @AuthenticationPrincipal UserDetails userDetails) {\n" +
                "    \n" +
                "    User teacher = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();\n" +
                "    Subject subject = subjectRepository.findById(subjectId).orElseThrow();\n" +
                "    if (!subject.getTeacher().getId().equals(teacher.getId())) {\n" +
                "        return ResponseEntity.status(403).body(\"Access Denied\");\n" +
                "    }\n" +
                "    LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();\n" +
                "    attendanceService.markAttendance(subjectId, date, request.getAttendance());\n" +
                "    return ResponseEntity.ok(\"Attendance marked successfully\");\n" +
                "}", codeFont);

            addParagraph(document, "API 3: Day-Wise Attendance History Endpoint (TeacherController.java)", boldBody);
            addCodeBlock(document, 
                "@GetMapping(\"/subjects/{subjectId}/date-attendance\")\n" +
                "public ResponseEntity<?> getDateAttendance(\n" +
                "        @PathVariable Long subjectId,\n" +
                "        @RequestParam(required = false) String date) {\n" +
                "    \n" +
                "    Subject subject = subjectRepository.findById(subjectId).orElseThrow();\n" +
                "    LocalDate targetDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();\n" +
                "    List<Attendance> logs = attendanceRepository.findBySubjectAndDate(subject, targetDate);\n" +
                "    \n" +
                "    Map<Long, AttendanceStatus> statusMap = new HashMap<>();\n" +
                "    for (Attendance a : logs) statusMap.put(a.getStudent().getId(), a.getStatus());\n" +
                "    \n" +
                "    List<Map<String, Object>> result = new ArrayList<>();\n" +
                "    for (User st : subject.getStudents()) {\n" +
                "        Map<String, Object> map = new HashMap<>();\n" +
                "        map.put(\"username\", st.getUsername());\n" +
                "        map.put(\"rollNumber\", st.getRollNumber());\n" +
                "        AttendanceStatus s = statusMap.get(st.getId());\n" +
                "        map.put(\"status\", s != null ? s.name() : \"NOT_MARKED\");\n" +
                "        result.add(map);\n" +
                "    }\n" +
                "    return ResponseEntity.ok(result);\n" +
                "}", codeFont);

            addParagraph(document, "API 4: Attendance Service Broadcasting & Database Marking (AttendanceService.java)", boldBody);
            addCodeBlock(document, 
                "@Service\n" +
                "public class AttendanceService {\n" +
                "    @Autowired private AttendanceRepository attendanceRepository;\n" +
                "    @Autowired private SimpMessagingTemplate messagingTemplate;\n" +
                "\n" +
                "    @Transactional\n" +
                "    public void markAttendance(Long subjectId, LocalDate date, Map<Long, AttendanceStatus> statusMap) {\n" +
                "        Subject subject = subjectRepository.findById(subjectId).orElseThrow();\n" +
                "        for (Map.Entry<Long, AttendanceStatus> entry : statusMap.entrySet()) {\n" +
                "            User student = userRepository.findById(entry.getKey()).orElseThrow();\n" +
                "            Attendance att = attendanceRepository.findBySubjectAndStudentAndDate(subject, student, date)\n" +
                "                    .orElse(new Attendance(student, subject, date, entry.getValue()));\n" +
                "            att.setStatus(entry.getValue());\n" +
                "            attendanceRepository.save(att);\n" +
                "            \n" +
                "            // Real-Time STOMP Broadcast to Teacher & Student\n" +
                "            messagingTemplate.convertAndSend(\"/topic/subject/\" + subjectId, att);\n" +
                "            messagingTemplate.convertAndSend(\"/topic/attendance/\" + student.getId(), att);\n" +
                "        }\n" +
                "    }\n" +
                "}", codeFont);

            document.add(Chunk.NEWLINE);

            // SECTION 4: DATABASE SCHEMAS & ENTITY MAPPINGS
            addSection(document, "SECTION 4: Database Schema & Entity Code", sectionHeadingFont);

            addParagraph(document, "Attendance Entity & Composite Unique Index (Attendance.java)", boldBody);
            addCodeBlock(document, 
                "@Entity\n" +
                "@Table(name = \"attendance\", uniqueConstraints = {\n" +
                "    @UniqueConstraint(name = \"unique_student_subject_date\", columnNames = {\"student_id\", \"subject_id\", \"date\"})\n" +
                "})\n" +
                "public class Attendance {\n" +
                "    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n" +
                "    private Long id;\n" +
                "\n" +
                "    @ManyToOne(fetch = FetchType.LAZY)\n" +
                "    @JoinColumn(name = \"student_id\", nullable = false)\n" +
                "    private User student;\n" +
                "\n" +
                "    @ManyToOne(fetch = FetchType.LAZY)\n" +
                "    @JoinColumn(name = \"subject_id\", nullable = false)\n" +
                "    private Subject subject;\n" +
                "\n" +
                "    @Column(nullable = false)\n" +
                "    private LocalDate date;\n" +
                "\n" +
                "    @Enumerated(EnumType.STRING)\n" +
                "    @Column(nullable = false)\n" +
                "    private AttendanceStatus status;\n" +
                "}", codeFont);

            document.close();
            System.out.println("Project Implementation PDF generated successfully at: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addSection(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private static void addParagraph(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static void addCodeBlock(Document doc, String code, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(code, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(6);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
        table.setSpacingAfter(6);
        doc.add(table);
    }

    private static void addQnA(Document doc, String q, String a) throws DocumentException {
        Font qFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, new Color(37, 99, 235));
        Font aFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, new Color(30, 41, 59));
        Paragraph pQ = new Paragraph(q, qFont);
        pQ.setSpacingBefore(6);
        doc.add(pQ);
        Paragraph pA = new Paragraph(a, aFont);
        pA.setSpacingAfter(6);
        doc.add(pA);
    }
}
