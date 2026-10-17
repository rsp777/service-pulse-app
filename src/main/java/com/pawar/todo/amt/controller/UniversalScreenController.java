package com.pawar.todo.amt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects legacy SDUI dashboard routes to the static single-page application.
 * All UI is now served from src/main/resources/static/index.html.
 */
@Controller
public class UniversalScreenController {

    @GetMapping({"/dashboard", "/dashboard/**", "/sdui-admin", "/alert-admin"})
    public String redirectToSpa() {
        return "redirect:/";
    }
}