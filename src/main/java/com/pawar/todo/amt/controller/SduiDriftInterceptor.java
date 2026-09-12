package com.pawar.todo.amt.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.pawar.todo.amt.service.UiActionCacheDriftMonitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SduiDriftInterceptor implements HandlerInterceptor {
    private final UiActionCacheDriftMonitor driftMonitor;
    public SduiDriftInterceptor(UiActionCacheDriftMonitor driftMonitor) { this.driftMonitor = driftMonitor; }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) modelAndView.addObject("driftDetected", driftMonitor.isDriftDetected());
    }
}