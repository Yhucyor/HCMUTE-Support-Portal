package com.school.counseling.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TicketController {

    @GetMapping("/ticket")
    public String ticketList() {
        return "ticket-list";
    }

    @GetMapping("/ticket/{id}")
    public String ticketDetail(@PathVariable String id) {
        return "ticket-detail";
    }
}
