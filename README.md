# GMU ERP

A college ERP MVP for GM University, with a React frontend, Spring Boot backend, and MySQL database.

## Modules

- Dashboard and role-aware navigation
- Students, faculty, departments, courses, and subjects
- Enrollment and attendance
- Exams, marks, and grade reports
- Fee invoices and payment status

## Run locally

1. Create the database using `database/schema.sql`.
2. Update MySQL credentials in `backend/src/main/resources/application.properties`.
3. In `backend`, run `mvn spring-boot:run`.
4. In `frontend`, run `npm install` then `npm run dev`.

The frontend currently uses sample data and is ready to connect to the API at `http://localhost:8080/api`.
