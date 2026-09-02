package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepo) {
        return args -> {
            // 1. HR Login Account
            User hr = new User();
            hr.setName("HR Admin");
            hr.setDesignation("HR Manager");
            hr.setEmail("hr@test.com");
            hr.setPassword("admin123");
            hr.setRole("HR");
            hr.setLeavesTaken(0.0);
            userRepo.save(hr);

            // 2. Employee Login Account
            User emp = new User();
            emp.setName("Kanish");
            emp.setDesignation("Software Engineer");
            emp.setEmail("Kanish@test.com");
            emp.setPassword("Kanish123");
            emp.setRole("EMPLOYEE");
            emp.setLeavesTaken(0.0);
            userRepo.save(emp);
        };
    }
}