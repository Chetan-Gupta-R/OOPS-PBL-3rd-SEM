package com.gmu.erp;

import java.util.Map;
import java.util.List;
import java.sql.Date;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "http://localhost:*")
public class HealthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public HealthController(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "application", "GMU ERP API"); }

    @GetMapping("/students")
    public List<Map<String, Object>> students() {
        return jdbc.queryForList("SELECT s.id, s.usn, u.full_name AS name, u.email, s.semester, c.name AS course, d.name AS department FROM students s JOIN users u ON u.id=s.user_id JOIN courses c ON c.id=s.course_id JOIN departments d ON d.id=s.department_id ORDER BY s.id DESC");
    }

    @PostMapping("/students")
    public Map<String, String> addStudent(@RequestBody StudentRequest request) {
        jdbc.update("INSERT IGNORE INTO departments(code,name) VALUES ('GEN','General Studies')");
        jdbc.update("INSERT IGNORE INTO courses(code,name,duration_years,department_id) SELECT 'GEN-UG','General Undergraduate',3,id FROM departments WHERE code='GEN'");
        Long departmentId = jdbc.queryForObject("SELECT id FROM departments WHERE code='GEN'", Long.class);
        Long courseId = jdbc.queryForObject("SELECT id FROM courses WHERE code='GEN-UG'", Long.class);
        Long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name='STUDENT'", Long.class);
        jdbc.update("INSERT INTO users(full_name,email,password_hash,role_id) VALUES (?,?,?,?)", request.name(), request.email(), encoder.encode("Student@123"), roleId);
        Long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO students(usn,user_id,department_id,course_id,semester,admission_date) VALUES (?,?,?,?,?,CURDATE())", request.usn(), userId, departmentId, courseId, request.semester());
        return Map.of("message", "Student added successfully");
    }

    @GetMapping("/courses")
    public List<Map<String, Object>> courses() { return jdbc.queryForList("SELECT c.id,c.code,c.name,c.duration_years AS durationYears,d.name AS department FROM courses c JOIN departments d ON d.id=c.department_id ORDER BY c.name"); }

    @PostMapping("/courses")
    public Map<String, String> addCourse(@RequestBody CourseRequest request) {
        jdbc.update("INSERT IGNORE INTO departments(code,name) VALUES (?,?)", request.departmentCode(), request.departmentName());
        Long departmentId = jdbc.queryForObject("SELECT id FROM departments WHERE code=?", Long.class, request.departmentCode());
        jdbc.update("INSERT INTO courses(code,name,duration_years,department_id) VALUES (?,?,?,?)", request.code(), request.name(), request.durationYears(), departmentId);
        return Map.of("message", "Course added successfully");
    }

    @GetMapping("/faculty")
    public List<Map<String, Object>> faculty() {
        return jdbc.queryForList("SELECT f.id,f.employee_code AS employeeCode,u.full_name AS name,u.email,f.designation,d.name AS department FROM faculty f JOIN users u ON u.id=f.user_id JOIN departments d ON d.id=f.department_id ORDER BY f.id DESC");
    }

    @PostMapping("/faculty")
    public Map<String, String> addFaculty(@RequestBody FacultyRequest request) {
        jdbc.update("INSERT IGNORE INTO departments(code,name) VALUES (?,?)", request.departmentCode(), request.departmentName());
        Long departmentId = jdbc.queryForObject("SELECT id FROM departments WHERE code=?", Long.class, request.departmentCode());
        Long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name='FACULTY'", Long.class);
        jdbc.update("INSERT INTO users(full_name,email,password_hash,role_id) VALUES (?,?,?,?)", request.name(), request.email(), encoder.encode("Faculty@123"), roleId);
        Long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO faculty(employee_code,user_id,department_id,designation) VALUES (?,?,?,?)", request.employeeCode(), userId, departmentId, request.designation());
        return Map.of("message", "Faculty member added successfully");
    }

    @GetMapping("/fees")
    public List<Map<String, Object>> fees(@RequestParam(required = false) String email) {
        String sql = "SELECT f.id,u.full_name AS student,f.description,f.amount,f.due_date AS dueDate,f.status FROM fee_invoices f JOIN students s ON s.id=f.student_id JOIN users u ON u.id=s.user_id";
        return email == null ? jdbc.queryForList(sql + " ORDER BY f.due_date") : jdbc.queryForList(sql + " WHERE u.email=? ORDER BY f.due_date", email);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Integer students = jdbc.queryForObject("SELECT COUNT(*) FROM students", Integer.class);
        Integer faculty = jdbc.queryForObject("SELECT COUNT(*) FROM faculty", Integer.class);
        Integer pendingInvoices = jdbc.queryForObject("SELECT COUNT(*) FROM fee_invoices WHERE status <> 'PAID'", Integer.class);
        return Map.of("students", students, "faculty", faculty, "pendingInvoices", pendingInvoices);
    }

    @PostMapping("/fees")
    public Map<String, String> addInvoice(@RequestBody InvoiceRequest request) {
        jdbc.update("INSERT INTO fee_invoices(student_id,academic_year,description,amount,due_date,status) VALUES (?,?,?,?,?,'PENDING')", request.studentId(), request.academicYear(), request.description(), request.amount(), Date.valueOf(request.dueDate()));
        return Map.of("message", "Fee invoice created successfully");
    }

    @PostMapping("/payments")
    public Map<String, String> addPayment(@RequestBody PaymentRequest request) {
        jdbc.update("INSERT INTO payments(invoice_id,amount,paid_at,reference_no,method) VALUES (?,?,NOW(),?,?)", request.invoiceId(), request.amount(), request.referenceNo(), request.method());
        jdbc.update("UPDATE fee_invoices f SET status=CASE WHEN (SELECT COALESCE(SUM(amount),0) FROM payments WHERE invoice_id=f.id)>=f.amount THEN 'PAID' ELSE 'PARTIAL' END WHERE f.id=?", request.invoiceId());
        return Map.of("message", "Payment recorded successfully");
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects() { return jdbc.queryForList("SELECT s.id,s.code,s.name,s.credits,s.semester,c.name AS course FROM subjects s JOIN courses c ON c.id=s.course_id ORDER BY s.code"); }

    @PostMapping("/subjects")
    public Map<String, String> addSubject(@RequestBody SubjectRequest request) {
        jdbc.update("INSERT INTO subjects(code,name,credits,semester,course_id) VALUES (?,?,?,?,?)", request.code(), request.name(), request.credits(), request.semester(), request.courseId());
        return Map.of("message", "Subject added successfully");
    }

    @GetMapping("/enrollments")
    public List<Map<String, Object>> enrollments() { return jdbc.queryForList("SELECT e.id,u.full_name AS student,s.name AS subject,e.academic_year AS academicYear FROM enrollments e JOIN students st ON st.id=e.student_id JOIN users u ON u.id=st.user_id JOIN subjects s ON s.id=e.subject_id ORDER BY e.id DESC"); }

    @PostMapping("/enrollments")
    public Map<String, String> addEnrollment(@RequestBody EnrollmentRequest request) {
        jdbc.update("INSERT INTO enrollments(student_id,subject_id,academic_year) VALUES (?,?,?)", request.studentId(), request.subjectId(), request.academicYear());
        return Map.of("message", "Student enrolled successfully");
    }

    @GetMapping("/attendance")
    public List<Map<String, Object>> attendance(@RequestParam(required = false) String email) {
        String sql = "SELECT a.id,u.full_name AS student,s.name AS subject,a.class_date AS classDate,a.status FROM attendance a JOIN enrollments e ON e.id=a.enrollment_id JOIN students st ON st.id=e.student_id JOIN users u ON u.id=st.user_id JOIN subjects s ON s.id=e.subject_id";
        return email == null ? jdbc.queryForList(sql + " ORDER BY a.class_date DESC") : jdbc.queryForList(sql + " WHERE u.email=? ORDER BY a.class_date DESC", email);
    }

    @PostMapping("/attendance")
    public Map<String, String> markAttendance(@RequestBody AttendanceRequest request) {
        Long facultyId = jdbc.queryForObject("SELECT id FROM faculty ORDER BY id LIMIT 1", Long.class);
        jdbc.update("INSERT INTO attendance(enrollment_id,class_date,status,marked_by) VALUES (?,?,?,?)", request.enrollmentId(), Date.valueOf(request.classDate()), request.status(), facultyId);
        return Map.of("message", "Attendance saved successfully");
    }

    @GetMapping("/exams")
    public List<Map<String, Object>> exams() { return jdbc.queryForList("SELECT id,name,academic_year AS academicYear,start_date AS startDate,end_date AS endDate FROM exams ORDER BY start_date DESC"); }

    @PostMapping("/exams")
    public Map<String, String> addExam(@RequestBody ExamRequest request) {
        jdbc.update("INSERT INTO exams(name,academic_year,start_date,end_date) VALUES (?,?,?,?)", request.name(), request.academicYear(), Date.valueOf(request.startDate()), Date.valueOf(request.endDate()));
        return Map.of("message", "Examination created successfully");
    }

    @GetMapping("/timetable")
    public List<Map<String, Object>> timetable(@RequestParam(required = false) String email, @RequestParam(required = false) String role) {
        String base = "SELECT cs.id,s.name AS subject,c.name AS course,u.full_name AS faculty,cs.semester,cs.room,cs.weekday,cs.start_time AS startTime,cs.end_time AS endTime FROM class_schedules cs JOIN subjects s ON s.id=cs.subject_id JOIN courses c ON c.id=s.course_id JOIN faculty f ON f.id=cs.faculty_id JOIN users u ON u.id=f.user_id";
        if ("STUDENT".equals(role)) return jdbc.queryForList(base + " JOIN students st ON st.course_id=s.course_id AND st.semester=cs.semester JOIN users su ON su.id=st.user_id WHERE su.email=? ORDER BY FIELD(cs.weekday,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'),cs.start_time", email);
        if ("FACULTY".equals(role)) return jdbc.queryForList(base + " WHERE u.email=? ORDER BY FIELD(cs.weekday,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'),cs.start_time", email);
        return jdbc.queryForList(base + " ORDER BY FIELD(cs.weekday,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'),cs.start_time");
    }

    @PostMapping("/timetable")
    public Map<String, String> addSchedule(@RequestBody ScheduleRequest request) {
        jdbc.update("INSERT INTO class_schedules(subject_id,faculty_id,semester,room,weekday,start_time,end_time) VALUES (?,?,?,?,?,?,?)", request.subjectId(), request.facultyId(), request.semester(), request.room(), request.weekday(), request.startTime(), request.endTime());
        return Map.of("message", "Class scheduled successfully");
    }

    @GetMapping("/marks")
    public List<Map<String, Object>> marks(@RequestParam(required = false) String email) {
        String sql = "SELECT m.id,u.full_name AS student,s.name AS subject,e.name AS exam,m.marks_obtained AS marksObtained,m.maximum_marks AS maximumMarks,m.grade FROM marks m JOIN enrollments en ON en.id=m.enrollment_id JOIN students st ON st.id=en.student_id JOIN users u ON u.id=st.user_id JOIN subjects s ON s.id=en.subject_id JOIN exams e ON e.id=m.exam_id";
        return email == null ? jdbc.queryForList(sql + " ORDER BY m.id DESC") : jdbc.queryForList(sql + " WHERE u.email=? ORDER BY m.id DESC", email);
    }

    @PostMapping("/marks")
    public Map<String, String> addMarks(@RequestBody MarksRequest request) {
        String grade = request.marksObtained() >= 90 ? "A+" : request.marksObtained() >= 75 ? "A" : request.marksObtained() >= 60 ? "B" : request.marksObtained() >= 40 ? "C" : "F";
        jdbc.update("INSERT INTO marks(exam_id,enrollment_id,maximum_marks,marks_obtained,grade) VALUES (?,?,?,?,?)", request.examId(), request.enrollmentId(), request.maximumMarks(), request.marksObtained(), grade);
        return Map.of("message", "Marks saved successfully");
    }

    public record StudentRequest(String name, String email, String usn, Integer semester) {}
    public record CourseRequest(String code, String name, Integer durationYears, String departmentCode, String departmentName) {}
    public record FacultyRequest(String name, String email, String employeeCode, String designation, String departmentCode, String departmentName) {}
    public record InvoiceRequest(Long studentId, String academicYear, String description, Double amount, String dueDate) {}
    public record PaymentRequest(Long invoiceId, Double amount, String referenceNo, String method) {}
    public record SubjectRequest(String code, String name, Integer credits, Integer semester, Long courseId) {}
    public record EnrollmentRequest(Long studentId, Long subjectId, String academicYear) {}
    public record AttendanceRequest(Long enrollmentId, String classDate, String status) {}
    public record ExamRequest(String name, String academicYear, String startDate, String endDate) {}
    public record MarksRequest(Long examId, Long enrollmentId, Double maximumMarks, Double marksObtained) {}
    public record ScheduleRequest(Long subjectId, Long facultyId, Integer semester, String room, String weekday, String startTime, String endTime) {}
}
