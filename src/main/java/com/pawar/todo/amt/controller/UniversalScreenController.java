package com.pawar.todo.amt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.pawar.todo.amt.service.UiActionService;

@Controller
public class UniversalScreenController {

    private final UiActionService uiActionService;

    public UniversalScreenController(UiActionService uiActionService) {
        this.uiActionService = uiActionService;
    }

    @GetMapping("/dashboard/{viewContext}")
    public String renderDashboard(@PathVariable String viewContext, Model model) {
        model.addAttribute("viewContext", viewContext);
        model.addAttribute("dashboardActions", uiActionService.getGroupedActions(viewContext));
        return "generic-screen";
    }
}