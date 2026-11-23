package com.gmu.erp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapData {
    @Bean
    CommandLineRunner seedDemoAccounts(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return args -> {
            createAccount(jdbc, encoder, "System Administrator", "admin@gmu.edu", "ADMIN", "Admin@123");
            createAccount(jdbc, encoder, "Dr. Priya Sharma", "faculty@gmu.edu", "FACULTY", "Faculty@123");
            createAccount(jdbc, encoder, "Aarav Kumar", "student@gmu.edu", "STUDENT", "Student@123");
            jdbc.update("INSERT IGNORE INTO departments(code,name) VALUES ('GEN','General Studies')");
            Long departmentId = jdbc.queryForObject("SELECT id FROM departments WHERE code='GEN'", Long.class);
            jdbc.update("INSERT IGNORE INTO courses(code,name,duration_years,department_id) VALUES ('GEN-UG','General Undergraduate',3,?)", departmentId);
            Long courseId = jdbc.queryForObject("SELECT id FROM courses WHERE code='GEN-UG'", Long.class);
            Long facultyUser = jdbc.queryForObject("SELECT id FROM users WHERE email='faculty@gmu.edu'", Long.class);
            Long studentUser = jdbc.queryForObject("SELECT id FROM users WHERE email='student@gmu.edu'", Long.class);
            jdbc.update("INSERT IGNORE INTO faculty(employee_code,user_id,department_id,designation) VALUES ('GMU-F001',?,?, 'Professor')", facultyUser, departmentId);
            jdbc.update("INSERT IGNORE INTO students(usn,user_id,department_id,course_id,semester,admission_date) VALUES ('GMU-S001',?,?,?,1,CURDATE())", studentUser, departmentId, courseId);
        };
    }

    private void createAccount(JdbcTemplate jdbc, PasswordEncoder encoder, String name, String email, String role, String password) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email);
        if (count == null || count == 0) {
            Long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE name=?", Long.class, role);
            jdbc.update("INSERT INTO users(full_name,email,password_hash,role_id,active) VALUES (?,?,?,?,true)", name, email, encoder.encode(password), roleId);
        }
    }
}
