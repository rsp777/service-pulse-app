package com.pawar.todo.amt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.pawar.todo.amt.service.UiActionService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UniversalScreenController {

    private final UiActionService uiActionService;

    public UniversalScreenController(UiActionService uiActionService) {
        this.uiActionService = uiActionService;
    }

    @GetMapping("/dashboard")
    public String defaultDashboard() {
        return "redirect:/dashboard/dashboard";
    }

    @GetMapping("/dashboard/{viewContext}")
    public String renderDashboard(@PathVariable String viewContext, Model model, HttpServletRequest request) {
        model.addAttribute("viewContext", viewContext);
        model.addAttribute("contextPath", request.getContextPath());
        model.addAttribute("dashboardContexts", uiActionService.getViewContexts());
        model.addAttribute("dashboardActions", uiActionService.getGroupedActions(viewContext));
        return "generic-screen";
    }

    @GetMapping("/sdui-admin")
    public String renderSduiAdmin(Model model) {
        return "sdui-admin";
    }

    @GetMapping("/alert-admin")
    public String renderAlertAdmin(Model model) {
        return "alert-admin";
    }
}