package com.example.demo;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class AppController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping("/")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session) {
        User user = userRepo.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user);
            return user.getRole().equalsIgnoreCase("HR") ? "redirect:/hr" : "redirect:/employee";
        }
        return "redirect:/?error=true";
    }

    @GetMapping("/employee")
    public String employeeDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";

        List<Attendance> history = attendanceRepo.findByUserOrderByIdDesc(user);
        Attendance today = attendanceRepo.findByUserAndDate(user, LocalDate.now()).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("history", history);
        model.addAttribute("todayRecord", today);
        return "employee";
    }

    @PostMapping("/employee/punch-in")
    public String punchIn(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && attendanceRepo.findByUserAndDate(user, LocalDate.now()).isEmpty()) {
            attendanceRepo.save(new Attendance(user, LocalDate.now(), LocalTime.now(), "IN"));
        }
        return "redirect:/employee";
    }

    @PostMapping("/employee/punch-out")
    public String punchOut(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            attendanceRepo.findByUserAndDate(user, LocalDate.now()).ifPresent(att -> {
                att.setCheckOutTime(LocalTime.now());
                att.setStatus("COMPLETED");
                attendanceRepo.save(att);
            });
        }
        return "redirect:/employee";
    }

    @GetMapping("/hr")
    public String hrDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().equalsIgnoreCase("HR")) return "redirect:/";

        List<User> employees = userRepo.findAll();
        List<Attendance> attendances = attendanceRepo.findAll();

        long presentToday = attendances.stream()
                .filter(a -> a.getDate().equals(LocalDate.now()))
                .count();

        model.addAttribute("user", user);
        model.addAttribute("employees", employees);
        model.addAttribute("attendances", attendances);
        model.addAttribute("totalStaff", employees.size());
        model.addAttribute("presentToday", presentToday);
        return "hr";
    }

    @PostMapping("/hr/add-employee")
    public String addEmployee(@RequestParam String name,
                              @RequestParam String designation,
                              @RequestParam String email,
                              @RequestParam String password,
                              HttpSession session) {
        User hr = (User) session.getAttribute("user");
        if (hr == null || !hr.getRole().equalsIgnoreCase("HR")) {
            return "redirect:/";
        }

        User emp = new User();
        emp.setName(name);
        emp.setDesignation(designation);
        emp.setEmail(email.trim().toLowerCase());
        emp.setPassword(password);
        emp.setRole("EMPLOYEE");
        emp.setLeavesTaken(0.0);
        userRepo.save(emp);

        return "redirect:/hr";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}