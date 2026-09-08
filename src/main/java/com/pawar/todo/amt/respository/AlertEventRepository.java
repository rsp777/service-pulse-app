package com.pawar.todo.amt.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawar.todo.amt.model.AlertEvent;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Integer> {
    List<AlertEvent> findTop50ByOrderByTriggeredDttmDesc();
}
