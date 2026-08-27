package com.clinic.repository_api.controller;

import com.clinic.repository_api.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view")
public class ViewController {

    private final ClientService clientService;

    public ViewController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/clients")
    public String listClients(Model model) {
        var clients = clientService.findAll();
        model.addAttribute("clients", clients);
        model.addAttribute("totalCount", clients.size());

        return "clients";
    }
}