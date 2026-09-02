package com.example.demo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class AppController {

    @Autowired 
    private UserRepository userRepo;

    @Autowired 
    private AttendanceRepository attRepo;

    @GetMapping("/")
    public String loginPage() { 
        return "login"; 
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password, HttpSession session) {
        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user);
            return user.getRole().equalsIgnoreCase("HR") ? "redirect:/hr" : "redirect:/employee";
        }
        return "redirect:/?error";
    }

    @GetMapping("/employee")
    public String empDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        
        Attendance todayAtt = attRepo.findByUserIdAndDate(user.getId(), LocalDate.now()).orElse(null);
        User currentUser = userRepo.findById(user.getId()).orElse(user);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("today", todayAtt);
        model.addAttribute("history", attRepo.findByUserId(user.getId()));
        return "employee";
    }

    @PostMapping("/checkin")
    public String checkIn(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";

        Attendance att = attRepo.findByUserIdAndDate(user.getId(), LocalDate.now()).orElse(new Attendance());
        att.setUserId(user.getId());
        att.setDate(LocalDate.now());
        att.setCheckIn(LocalDateTime.now());
        att.setStatus("PRESENT");
        attRepo.save(att);
        return "redirect:/employee";
    }

    @PostMapping("/checkout")
    public String checkOut(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";

        Attendance att = attRepo.findByUserIdAndDate(user.getId(), LocalDate.now()).orElse(null);
        if (att != null && att.getCheckIn() != null && att.getCheckOut() == null) {
            att.setCheckOut(LocalDateTime.now());
            
            long minutes = Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes();
            att.setWorkingHours((double) minutes);

            User dbUser = userRepo.findById(user.getId()).orElse(user);
            if (minutes < 240) { 
                att.setStatus("ABSENT"); 
                dbUser.setLeavesTaken(dbUser.getLeavesTaken() + 1.0); 
            } else if (minutes < 480) { 
                att.setStatus("HALF_DAY"); 
                dbUser.setLeavesTaken(dbUser.getLeavesTaken() + 0.5); 
            } else { 
                att.setStatus("PRESENT"); 
            }
            
            userRepo.save(dbUser);
            attRepo.save(att);
        }
        return "redirect:/employee";
    }

    @GetMapping("/hr")
    public String hrDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().equalsIgnoreCase("HR")) {
            return "redirect:/";
        }
        model.addAttribute("records", attRepo.findAll());
        model.addAttribute("employees", userRepo.findAll());
        return "hr";
    }

    // HR द्वारा नया Employee ऐड करने का एंडपॉइंट
    @PostMapping("/hr/add-employee")
    public String addEmployee(
            @RequestParam String name,
            @RequestParam String designation,
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {
        
        User hr = (User) session.getAttribute("user");
        if (hr == null || !hr.getRole().equalsIgnoreCase("HR")) {
            return "redirect:/";
        }

        User newEmp = new User();
        newEmp.setName(name);
        newEmp.setDesignation(designation);
        newEmp.setEmail(email);
        newEmp.setPassword(password);
        newEmp.setRole("EMPLOYEE");
        newEmp.setLeavesTaken(0.0);

        userRepo.save(newEmp);
        return "redirect:/hr?success";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}