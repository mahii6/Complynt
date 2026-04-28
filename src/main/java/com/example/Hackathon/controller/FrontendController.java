package com.example.Hackathon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/complaints")
    public String complaints() {
        return "complaints";
    }

    @GetMapping("/complaint-detail")
    public String complaintDetail() {
        return "complaint-detail";
    }

    @GetMapping("/new-complaint")
    public String newComplaint() {
        return "new-complaint";
    }

    @GetMapping("/agents")
    public String agents() {
        return "agents";
    }

    @GetMapping("/customers")
    public String customers() {
        return "customers";
    }

    @GetMapping("/sla")
    public String sla() {
        return "sla";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }
}
