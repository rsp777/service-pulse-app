package com.pawar.todo.amt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ui_actions")
public class UiAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ui_action_id")
    private Integer id;

    @Column(name = "view_context", nullable = false, length = 64)
    private String viewContext;

    @Column(name = "action_label", nullable = false, length = 128)
    private String actionLabel;

    @Column(name = "action_endpoint", nullable = false, length = 255)
    private String actionEndpoint;

    @Column(name = "request_payload", columnDefinition = "json")
    private String requestPayload;

    @Column(name = "sidebar_category", length = 64)
    private String sidebarCategory;

    @Column(name = "panel_title", length = 64)
    private String panelTitle;

    @Column(name = "grid_span", nullable = false, length = 16)
    private String gridSpan = "span-12";

    @Column(name = "icon_class", length = 64)
    private String iconClass;
}